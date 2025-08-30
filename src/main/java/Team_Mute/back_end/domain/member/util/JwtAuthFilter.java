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

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwt;
	private final SessionStore store;

	public JwtAuthFilter(JwtService jwt, SessionStore store) {
		this.jwt = jwt;
		this.store = store;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
		throws ServletException, IOException {

		String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
		if (auth == null || !auth.startsWith("Bearer ")) {
			log.debug("Authorization 헤더가 없거나 'Bearer '로 시작하지 않습니다. URI: {}", req.getRequestURI());
			chain.doFilter(req, res);
			return;
		}

		String token = auth.substring(7);
		log.info("인증 토큰 발견: {}", token);

		try {
			Jws<Claims> jws = jwt.parse(token);
			Claims c = jws.getPayload();
			log.info("토큰 파싱 성공. Subject(userId): {}, Audience: {}", c.getSubject(), c.getAudience());

			String jti = c.getId();
			String sid = (String)c.get("sid");

			// 토큰 유효성 검사 (블랙리스트, 세션)
			if (jti != null && store.isBlacklisted(jti)) {
				log.warn("블랙리스트에 등록된 토큰입니다. JTI: {}", jti);
				// 401 응답 대신, 인증 없이 다음 필터로 진행
			} else if (sid == null || store.getSessionJson(sid) == null) {
				log.warn("유효하지 않은 세션 ID입니다. SID: {}", sid);
				// 401 응답 대신, 인증 없이 다음 필터로 진행
			} else if (c.getAudience().contains("user-service")) {
				processUserToken(c);
				log.info("사용자 인증 성공. SecurityContext에 저장 완료.");
			} else if (c.getAudience().contains("admin-service")) {
				processAdminToken(c);
				log.info("관리자 인증 성공. SecurityContext에 저장 완료.");
			} else {
				log.warn("알 수 없는 Audience 입니다: {}", c.getAudience());
				// 401 응답 대신, 인증 없이 다음 필터로 진행
			}
		} catch (Exception e) {
			// JWT 파싱/검증 자체에서 예외 발생 시 (예: 만료, 서명 오류 등)
			log.error("토큰 검증 중 예외 발생: {}", e.getMessage());
			// SecurityContext를 비워 인증되지 않은 상태로 만듬
			SecurityContextHolder.clearContext();
		}

		// 모든 경우에 대해 다음 필터 체인을 실행
		chain.doFilter(req, res);
	}

	private void processUserToken(Claims claims) {
		String userId = claims.getSubject();
		Integer roleId = claims.get("roles", Integer.class);

		Collection<GrantedAuthority> auths = new ArrayList<>();
		if (roleId != null) {
			auths.add(new SimpleGrantedAuthority("ROLE_" + roleId));
		}

		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userId, null, auths);
		authentication.setDetails(Map.of(
			"cid", claims.get("cid"),
			"sid", claims.get("sid"),
			"ver", claims.get("ver")
		));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void processAdminToken(Claims claims) {
		String userId = claims.getSubject();
		Integer roleId = claims.get("roleId", Integer.class);

		Collection<GrantedAuthority> auths = new ArrayList<>();
		if (roleId != null) {
			auths.add(new SimpleGrantedAuthority("ROLE_" + roleId));
		}

		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userId, null, auths);

		Map<String, Object> details = new HashMap<>();
		details.put("sid", claims.get("sid"));
		details.put("ver", claims.get("ver"));

		Integer regionId = claims.get("regionId", Integer.class);
		if (regionId != null) {
			details.put("regionId", regionId);
		}

		authentication.setDetails(details);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
