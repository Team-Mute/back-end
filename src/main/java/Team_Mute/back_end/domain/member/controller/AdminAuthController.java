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
import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.jwt.TokenPair;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.member.service.AdminAuthService;
import Team_Mute.back_end.domain.member.service.AuthService;
import Team_Mute.back_end.domain.member.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 인증 API", description = "관리자 인증 관련 API 명세")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

	private final AdminAuthService adminAuthService;
	private final UserRepository userRepository;
	private final PasswordService passwordService;
	private final AuthService userAuthService;

	private static final String RT_COOKIE_NAME = "admin_refresh_token";
	private static final String RT_COOKIE_PATH = "/api/admin/auth/refresh";

	@Value("${jwt.refresh-token.ttl-seconds}")
	private long rtTtlSeconds;

	@Operation(summary = "관리자 로그인", description = "이메일과 비밀번호를 받아 로그인을 진행합니다.")
	@PostMapping("/login")
	public ResponseEntity<AdminLoginDto.Response> login(@Valid @RequestBody AdminLoginDto.Request req,
		HttpServletResponse res) {
		User admin = userRepository.findByUserEmail(req.email())
			.orElse(null);

		final List<Integer> ADMIN_ROLES = Arrays.asList(0, 1, 2);

		if (admin == null || !ADMIN_ROLES.contains(admin.getUserRole().getRoleId())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		if (!passwordService.matches(req.password(), admin.getUserPwd())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		TokenPair pair = adminAuthService.login(admin);

		addRefreshCookie(res, pair.refreshToken(), (int)rtTtlSeconds);

		return ResponseEntity.ok(new AdminLoginDto.Response(pair.accessToken()));
	}

	@Operation(summary = "관리자 토큰 재발급", description = "토큰을 확인하여 로테이션 전략에 기반하여 토큰을 재발급합니다.")
	@PostMapping("/refresh")
	public ResponseEntity<AdminLoginDto.Response> refresh(
		@CookieValue(name = RT_COOKIE_NAME, required = false) String refreshToken,
		HttpServletResponse res
	) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		TokenPair pair = adminAuthService.refresh(refreshToken);
		addRefreshCookie(res, pair.refreshToken(), (int)rtTtlSeconds);

		return ResponseEntity.ok(new AdminLoginDto.Response(pair.accessToken()));
	}

	@Operation(summary = "관리자 로그아웃", description = "토큰을 확인하여 해당 관리자를 로그아웃합니다.")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
		Authentication authCtx,
		@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
		HttpServletResponse res) {
		if (authCtx == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		String userId = (String)authCtx.getPrincipal();
		String at = authorization.substring(7);

		userAuthService.logout(at, userId);

		clearRefreshCookie(res);
		return ResponseEntity.noContent().build();
	}

	private void addRefreshCookie(HttpServletResponse res, String refreshToken, int maxAgeSeconds) {
		ResponseCookie cookie = ResponseCookie.from(RT_COOKIE_NAME, refreshToken)
			.httpOnly(true)
			.secure(true)
			.path(RT_COOKIE_PATH)
			.maxAge(Duration.ofSeconds(maxAgeSeconds))
			.sameSite("Lax")
			.build();
		res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

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
