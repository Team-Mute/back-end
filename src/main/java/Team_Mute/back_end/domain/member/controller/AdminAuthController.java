package Team_Mute.back_end.domain.member.controller;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Team_Mute.back_end.domain.member.dto.request.AdminLoginDto;
import Team_Mute.back_end.domain.member.dto.response.AdminRefreshResponseDto;
import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.jwt.TokenPair;
import Team_Mute.back_end.domain.member.repository.AdminRepository;
import Team_Mute.back_end.domain.member.service.AdminAuthService;
import Team_Mute.back_end.domain.member.service.AuthService;
import Team_Mute.back_end.domain.member.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 인증 관련 요청을 처리하는 컨트롤러입니다.
 * JWT 기반 인증 시스템을 사용하며, 로그인, 토큰 재발급, 로그아웃 기능을 제공
 * Refresh Token은 HttpOnly 쿠키로 관리하여 XSS 공격으로부터 보호
 * Access Token은 응답 바디로 전달하여 클라이언트가 HTTP 헤더에 포함하여 API 요청 시 사용
 * RTR(Refresh Token Rotation) 전략을 채택하여 토큰 재발급 시마다 새로운 Refresh Token 생성
 *
 * @author Team Mute
 * @since 1.0
 */
@Slf4j
@Tag(name = "관리자 인증 API", description = "관리자 인증 관련 API 명세")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

	private final AdminAuthService adminAuthService;
	private final AdminRepository adminRepository;
	private final PasswordService passwordService;
	private final AuthService userAuthService;

	/**
	 * Refresh Token 쿠키 이름
	 * - 관리자용 Refresh Token을 저장하는 쿠키의 고유 식별자
	 */
	private static final String RT_COOKIE_NAME = "admin_refresh_token";

	/**
	 * Refresh Token 쿠키 경로
	 * - 쿠키가 유효한 경로를 제한하여 보안 강화
	 * - /api/admin/auth/refresh 엔드포인트에서만 쿠키 전송
	 */
	private static final String RT_COOKIE_PATH = "/api/admin/auth/refresh";

	/**
	 * Refresh Token 만료 시간 (초 단위)
	 * - application.properties의 jwt.refresh-token.ttl-seconds 값을 주입
	 */
	@Value("${jwt.refresh-token.ttl-seconds}")
	private long rtTtlSeconds;

	/**
	 * 관리자 로그인
	 * - 이메일과 비밀번호를 검증하여 관리자 인증 수행
	 * - 인증 성공 시 Access Token은 응답 바디에, Refresh Token은 HttpOnly 쿠키에 저장
	 * - 관리자 권한(roleId 0, 1, 2)만 로그인 허용
	 * - BCrypt 알고리즘으로 비밀번호 일치 여부 검증
	 *
	 * @param req 로그인 요청 DTO (이메일, 비밀번호 포함)
	 * @param res HttpServletResponse (Refresh Token 쿠키 설정에 사용)
	 * @return Access Token과 관리자 권한 정보를 포함하는 {@code ResponseEntity<AdminLoginDto.Response>}
	 */
	@Operation(summary = "관리자 로그인", description = "이메일과 비밀번호를 받아 로그인을 진행합니다.")
	@PostMapping("/login")
	public ResponseEntity<AdminLoginDto.Response> login(
		@Valid @RequestBody AdminLoginDto.Request req,
		HttpServletResponse res
	) {
		// 1. 이메일로 관리자 정보 조회
		Admin admin = adminRepository.findByAdminEmail(req.email())
			.orElse(null);

		// 2. 관리자 권한 검증 (0: 최고 관리자, 1: 2차 승인자, 2: 1차 승인자)
		final List<Integer> ADMIN_ROLES = Arrays.asList(0, 1, 2);

		if (admin == null || !ADMIN_ROLES.contains(admin.getUserRole().getRoleId())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 3. 비밀번호 일치 여부 검증 (BCrypt 해시 비교)
		if (!passwordService.matches(req.password(), admin.getAdminPwd())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 4. JWT 토큰 쌍(Access Token, Refresh Token) 생성
		TokenPair pair = adminAuthService.login(admin);

		// 5. Refresh Token을 HttpOnly 쿠키에 저장 (XSS 공격 방지)
		addRefreshCookie(res, pair.refreshToken(), (int)rtTtlSeconds);

		// 6. Access Token과 권한 정보를 응답 바디로 반환
		AdminLoginDto.Response responseBody = new AdminLoginDto.Response(
			pair.accessToken(),
			admin.getUserRole().getRoleId()
		);

		return ResponseEntity.ok(responseBody);
	}

	/**
	 * 관리자 토큰 재발급
	 * - Refresh Token을 검증하여 새로운 Access Token과 Refresh Token 발급
	 * - RTR(Refresh Token Rotation) 전략: 토큰 재발급 시마다 새로운 Refresh Token 생성하여 보안 강화
	 * - Refresh Token은 쿠키에서 자동으로 추출
	 * - 토큰이 유효하지 않거나 만료된 경우 401 Unauthorized 반환
	 *
	 * @param refreshToken 쿠키에서 추출한 Refresh Token (자동 주입)
	 * @param res HttpServletResponse (새로운 Refresh Token 쿠키 설정에 사용)
	 * @return 새로운 Access Token을 포함하는 {@code ResponseEntity<AdminRefreshResponseDto>}
	 */
	@Operation(summary = "관리자 토큰 재발급", description = "토큰을 확인하여 로테이션 전략에 기반하여 토큰을 재발급합니다.")
	@PostMapping("/refresh")
	public ResponseEntity<AdminRefreshResponseDto> refresh(
		@CookieValue(name = RT_COOKIE_NAME, required = false) String refreshToken,
		HttpServletResponse res
	) {
		// 1. Refresh Token 존재 여부 검증
		if (refreshToken == null || refreshToken.isBlank()) {
			log.info("refreshToken이 없어용!");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 2. Refresh Token 검증 및 새로운 토큰 쌍 생성 (RTR 전략)
		TokenPair pair = adminAuthService.refresh(refreshToken);

		// 3. 새로운 Refresh Token을 쿠키에 저장
		addRefreshCookie(res, pair.refreshToken(), (int)rtTtlSeconds);

		// 4. 새로운 Access Token을 응답 바디로 반환
		AdminRefreshResponseDto responseBody = new AdminRefreshResponseDto(pair.accessToken());

		return ResponseEntity.ok(responseBody);
	}

	/**
	 * 관리자 로그아웃
	 * - Access Token을 블랙리스트에 등록하여 해당 토큰으로 더 이상 API 접근 불가하도록 처리
	 * - Refresh Token 쿠키 삭제 (maxAge=0 설정)
	 * - Redis에 저장된 Refresh Token 정보 제거
	 * - 인증되지 않은 요청인 경우 401 Unauthorized 반환
	 *
	 * @param authCtx Spring Security의 인증 컨텍스트 (관리자 ID 포함)
	 * @param authorization Authorization 헤더 (Bearer {Access Token} 형식)
	 * @param res HttpServletResponse (Refresh Token 쿠키 삭제에 사용)
	 * @return 204 No Content 응답
	 */
	@Operation(summary = "관리자 로그아웃", description = "토큰을 확인하여 해당 관리자를 로그아웃합니다.")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
		Authentication authCtx,
		@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
		HttpServletResponse res
	) {
		// 1. 인증 정보 존재 여부 검증
		if (authCtx == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 2. 인증 컨텍스트에서 사용자 ID 추출
		String userId = (String)authCtx.getPrincipal();

		// 3. Authorization 헤더에서 "Bearer " 제거하여 순수 Access Token 추출
		String at = authorization.substring(7);

		// 4. Access Token 블랙리스트 등록 및 Redis의 Refresh Token 정보 삭제
		userAuthService.logout(at, userId);

		// 5. Refresh Token 쿠키 삭제
		clearRefreshCookie(res);

		return ResponseEntity.noContent().build();
	}

	/**
	 * Refresh Token 쿠키 추가
	 * - HttpOnly, Secure, SameSite=None 속성을 설정하여 보안 강화
	 * - HttpOnly: JavaScript에서 쿠키 접근 불가 (XSS 공격 방지)
	 * - Secure: HTTPS 연결에서만 쿠키 전송
	 * - SameSite=None: Cross-Origin 요청에서도 쿠키 전송 허용 (프론트엔드가 다른 도메인인 경우)
	 * - Path 제한: /api/admin/auth/refresh 경로에서만 쿠키 전송
	 *
	 * @param res HttpServletResponse 객체
	 * @param refreshToken 저장할 Refresh Token 문자열
	 * @param maxAgeSeconds 쿠키 만료 시간 (초 단위)
	 */
	private void addRefreshCookie(HttpServletResponse res, String refreshToken, int maxAgeSeconds) {
		ResponseCookie cookie = ResponseCookie.from(RT_COOKIE_NAME, refreshToken)
			.httpOnly(true)
			.secure(true)
			.path(RT_COOKIE_PATH)
			.maxAge(Duration.ofSeconds(maxAgeSeconds))
			.sameSite("None")
			.build();
		res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	/**
	 * Refresh Token 쿠키 삭제
	 * - 로그아웃 시 쿠키를 무효화하기 위해 maxAge를 0으로 설정
	 * - 빈 문자열을 값으로 설정하여 기존 쿠키 덮어쓰기
	 * - SameSite를 Lax로 설정 (삭제 시에는 더 제한적인 정책 적용)
	 *
	 * @param res HttpServletResponse 객체
	 */
	private void clearRefreshCookie(HttpServletResponse res) {
		ResponseCookie cookie = ResponseCookie.from(RT_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(true)
			.path(RT_COOKIE_PATH)
			.maxAge(Duration.ZERO)
			.sameSite("Lax")
			.build();
		res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
