package Team_Mute.back_end.domain.member.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import Team_Mute.back_end.domain.member.jwt.JwtService;
import Team_Mute.back_end.domain.member.session.SessionStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 인증 필터 클래스
 * Spring Security의 OncePerRequestFilter를 확장하여 JWT 기반 인증 처리
 * 모든 HTTP 요청마다 한 번씩 실행되어 JWT 토큰을 검증하고 SecurityContext에 인증 정보 설정
 *
 * 주요 기능:
 * - Authorization 헤더에서 Bearer Token 추출
 * - JWT 토큰 파싱 및 서명 검증
 * - Access Token JTI 블랙리스트 확인 (로그아웃된 토큰)
 * - 세션 유효성 검증 (Redis)
 * - Audience 기반 사용자/관리자 구분
 * - SecurityContext에 인증 정보 설정
 * - Spring Security의 @PreAuthorize, @Secured 등과 연동
 *
 * 필터 체인 위치:
 * - Spring Security Filter Chain 중간에 위치
 * - UsernamePasswordAuthenticationFilter 이전에 실행
 * - SecurityConfig에서 addFilterBefore()로 등록
 *
 * 보안 정책:
 * - 토큰 검증 실패 시 SecurityContext를 비워 인증되지 않은 상태로 처리
 * - 401 Unauthorized 응답은 Spring Security가 자동 처리
 * - 블랙리스트 및 세션 검증으로 즉시 로그아웃 지원
 *
 * @author Team Mute
 * @since 1.0
 */
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwt;
	private final SessionStore store;

	public JwtAuthFilter(JwtService jwt, SessionStore store) {
		this.jwt = jwt;
		this.store = store;
	}

	/**
	 * HTTP 요청마다 실행되는 필터 메서드
	 * - OncePerRequestFilter를 상속하여 요청당 한 번만 실행 보장
	 * - JWT 토큰 추출, 검증, SecurityContext 설정
	 *
	 * 처리 흐름:
	 * 1. Authorization 헤더에서 Bearer Token 추출
	 * 2. 토큰이 없거나 형식이 잘못된 경우 다음 필터로 진행
	 * 3. JWT 토큰 파싱 및 서명 검증
	 * 4. JTI 블랙리스트 확인 (로그아웃된 토큰)
	 * 5. 세션 유효성 검증 (Redis)
	 * 6. Audience 확인 (user-service 또는 admin-service)
	 * 7. 사용자/관리자 토큰 처리 (SecurityContext 설정)
	 * 8. 검증 실패 시 SecurityContext 비우고 다음 필터로 진행
	 *
	 * 보안 정책:
	 * - 토큰 검증 실패 시 401 응답 대신 인증 없이 진행
	 * - Spring Security가 최종적으로 접근 제어 수행
	 * - @PreAuthorize, @Secured 등의 어노테이션과 연동
	 *
	 * @param req HttpServletRequest 객체
	 * @param res HttpServletResponse 객체
	 * @param chain FilterChain 객체 (다음 필터 실행)
	 * @throws ServletException 서블릿 예외
	 * @throws IOException 입출력 예외
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
		throws ServletException, IOException {

		// 1. Authorization 헤더 추출
		String auth = req.getHeader(HttpHeaders.AUTHORIZATION);

		// 2. Authorization 헤더 검증
		if (auth == null || !auth.startsWith("Bearer ")) {
			log.debug("Authorization 헤더가 없거나 'Bearer '로 시작하지 않습니다. URI: {}", req.getRequestURI());
			chain.doFilter(req, res);  // 다음 필터로 진행 (인증 없음)
			return;
		}

		// 3. Bearer Token 추출 (앞의 "Bearer " 제거)
		String token = auth.substring(7);
		log.info("인증 토큰 발견: {}", token);

		try {
			// 4. JWT 토큰 파싱 및 서명 검증
			Jws<Claims> jws = jwt.parse(token);
			Claims c = jws.getPayload();
			log.info("토큰 파싱 성공. Subject(userId): {}, Audience: {}", c.getSubject(), c.getAudience());

			// 5. JWT 클레임에서 필요한 정보 추출
			String jti = c.getId();           // JWT ID
			String sid = (String)c.get("sid"); // 세션 ID

			// 6. 토큰 유효성 검사
			if (jti != null && store.isBlacklisted(jti)) {
				// 6-1. 블랙리스트에 등록된 토큰 (로그아웃된 토큰)
				log.warn("블랙리스트에 등록된 토큰입니다. JTI: {}", jti);
				// 인증 없이 다음 필터로 진행 (Spring Security가 401 처리)
			} else if (sid == null || store.getSessionJson(sid) == null) {
				// 6-2. 유효하지 않은 세션 (세션이 없거나 만료됨)
				log.warn("유효하지 않은 세션 ID입니다. SID: {}", sid);
				// 인증 없이 다음 필터로 진행
			} else if (c.getAudience().contains("user-service")) {
				// 6-3. 일반 사용자 토큰 처리
				processUserToken(c);
				log.info("사용자 인증 성공. SecurityContext에 저장 완료.");
			} else if (c.getAudience().contains("admin-service")) {
				// 6-4. 관리자 토큰 처리
				processAdminToken(c);
				log.info("관리자 인증 성공. SecurityContext에 저장 완료.");
			} else {
				// 6-5. 알 수 없는 Audience
				log.warn("알 수 없는 Audience 입니다: {}", c.getAudience());
				// 인증 없이 다음 필터로 진행
			}
		} catch (Exception e) {
			// 7. JWT 파싱/검증 예외 처리 (만료, 서명 오류 등)
			log.error("토큰 검증 중 예외 발생: {}", e.getMessage());

			// SecurityContext를 비워 인증되지 않은 상태로 만듬
			SecurityContextHolder.clearContext();
		}

		// 8. 모든 경우에 다음 필터 체인 실행
		chain.doFilter(req, res);
	}

	/**
	 * 일반 사용자 토큰 처리
	 * - JWT 클레임에서 사용자 정보 추출
	 * - Spring Security의 Authentication 객체 생성
	 * - SecurityContext에 인증 정보 설정
	 *
	 * 처리 내용:
	 * 1. Subject(userId) 추출
	 * 2. roleId 추출 (roles 클레임)
	 * 3. GrantedAuthority 생성 (ROLE_{roleId} 형식)
	 * 4. UsernamePasswordAuthenticationToken 생성
	 * 5. Details에 추가 정보 저장 (cid, sid, ver)
	 * 6. SecurityContext에 인증 정보 설정
	 *
	 * SecurityContext 구조:
	 * - Principal: userId (String)
	 * - Credentials: null (JWT는 비밀번호 불필요)
	 * - Authorities: [ROLE_3] (일반 사용자)
	 * - Details: {cid, sid, ver}
	 *
	 * 사용 예시:
	 * - @PreAuthorize("hasRole('3')"): 일반 사용자만 접근
	 * - SecurityContextHolder.getContext().getAuthentication().getPrincipal(): userId 조회
	 *
	 * @param claims JWT Claims 객체
	 */
	private void processUserToken(Claims claims) {
		// 1. Subject(userId) 추출
		String userId = claims.getSubject();

		// 2. roleId 추출
		Integer roleId = claims.get("roles", Integer.class);

		// 3. GrantedAuthority 생성
		Collection<GrantedAuthority> auths = new ArrayList<>();
		if (roleId != null) {
			// ROLE_ 접두사 추가 (Spring Security 규칙)
			auths.add(new SimpleGrantedAuthority("ROLE_" + roleId));
		}

		// 4. UsernamePasswordAuthenticationToken 생성
		// - principal: userId
		// - credentials: null (JWT는 비밀번호 불필요)
		// - authorities: [ROLE_3]
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userId, null, auths);

		// 5. Details에 추가 정보 저장
		authentication.setDetails(Map.of(
			"cid", claims.get("cid"),   // 소속 기업 ID
			"sid", claims.get("sid"),   // 세션 ID
			"ver", claims.get("ver")    // Token Version
		));

		// 6. SecurityContext에 인증 정보 설정
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	/**
	 * 관리자 토큰 처리
	 * - JWT 클레임에서 관리자 정보 추출
	 * - Spring Security의 Authentication 객체 생성
	 * - SecurityContext에 인증 정보 설정
	 *
	 * 처리 내용:
	 * 1. Subject(adminId) 추출
	 * 2. roleId 추출 (관리자 역할: 0, 1, 2)
	 * 3. GrantedAuthority 생성 (ROLE_{roleId} 형식)
	 * 4. UsernamePasswordAuthenticationToken 생성
	 * 5. Details에 추가 정보 저장 (sid, ver, regionId)
	 * 6. SecurityContext에 인증 정보 설정
	 *
	 * SecurityContext 구조:
	 * - Principal: adminId (String)
	 * - Credentials: null (JWT는 비밀번호 불필요)
	 * - Authorities: [ROLE_0] (마스터), [ROLE_1] (2차 승인자), [ROLE_2] (1차 승인자)
	 * - Details: {sid, ver, regionId}
	 *
	 * 사용 예시:
	 * - @PreAuthorize("hasRole('0')"): 마스터 관리자만 접근
	 * - @PreAuthorize("hasAnyRole('0', '1', '2')"): 모든 관리자 접근
	 * - SecurityContextHolder.getContext().getAuthentication().getPrincipal(): adminId 조회
	 *
	 * @param claims JWT Claims 객체
	 */
	private void processAdminToken(Claims claims) {
		// 1. Subject(adminId) 추출
		String userId = claims.getSubject();

		// 2. roleId 추출 (관리자 역할)
		Integer roleId = claims.get("roleId", Integer.class);

		// 3. GrantedAuthority 생성
		Collection<GrantedAuthority> auths = new ArrayList<>();
		if (roleId != null) {
			// ROLE_ 접두사 추가 (Spring Security 규칙)
			auths.add(new SimpleGrantedAuthority("ROLE_" + roleId));
		}

		// 4. UsernamePasswordAuthenticationToken 생성
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userId, null, auths);

		// 5. Details 맵 생성
		Map<String, Object> details = new HashMap<>();
		details.put("sid", claims.get("sid"));   // 세션 ID
		details.put("ver", claims.get("ver"));   // Token Version

		// 6. regionId 추출 (마스터 관리자는 -1 또는 null)
		Integer regionId = claims.get("regionId", Integer.class);
		if (regionId != null) {
			details.put("regionId", regionId);
		}

		// 7. Details 설정
		authentication.setDetails(details);

		// 8. SecurityContext에 인증 정보 설정
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
