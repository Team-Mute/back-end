package Team_Mute.back_end.domain.member.controller;

import java.time.Duration;

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

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.jwt.JwtService;
import Team_Mute.back_end.domain.member.jwt.TokenPair;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.member.service.AuthService;
import Team_Mute.back_end.domain.member.service.PasswordService;
import Team_Mute.back_end.domain.member.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@Tag(name = "사용자 인증 API", description = "사용자 인증 관련 API 명세")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final String RT_COOKIE_NAME = "refresh_token";
	private static final String RT_COOKIE_PATH = "/api/auth/refresh";

	private final AuthService auth;
	private final JwtService jwtService;
	private final UserService userService;
	private final UserRepository userRepository;
	private final PasswordService passwordService;

	@Value("${jwt.refresh-token.ttl-seconds}")
	private long rtTtlSeconds;

	public record LoginReq(@NotBlank String email, @NotBlank String password, String device, String ip) {
	}

	public record TokenResp(String accessToken) {
	}

	public record RefreshResp(String accessToken) {
	}

	@Operation(summary = "사용자 로그인", description = "이메일과 비밀번호를 받아 로그인을 진행합니다.")
	@PostMapping("/login")
	public ResponseEntity<TokenResp> login(@RequestBody LoginReq req, HttpServletResponse res) {
		User user = userRepository.findByUserEmail(req.email()).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		if (!passwordService.matches(req.password(), user.getUserPwd())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String userId = String.valueOf(user.getUserId());
		String companyId = String.valueOf(user.getCompanyId());
		Integer roles = user.getRoleId();
		Integer ver = user.getTokenVer();

		TokenPair pair = auth.login(
			userId,
			companyId,
			ver,
			roles,
			(req.device() == null ? "unknown" : req.device()),
			(req.ip() == null ? "0.0.0.0" : req.ip())
		);

		addRefreshCookie(res, pair.refreshToken(), (int)rtTtlSeconds);
		return ResponseEntity.ok(new TokenResp(pair.accessToken()));
	}

	@Operation(summary = "토큰 재발급", description = "로테이션 전략에 기반하여 토큰을 재발급합니다.")
	@PostMapping("/refresh")
	public ResponseEntity<RefreshResp> refresh(
		@CookieValue(name = RT_COOKIE_NAME, required = false) String refreshToken,
		HttpServletResponse res
	) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Jws<Claims> jws = jwtService.parse(refreshToken);
		String userIdStr = jws.getPayload().getSubject();
		if (userIdStr == null || userIdStr.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		Long userId = Long.valueOf(userIdStr);

		Integer currentVerExpected = userService.getTokenVer(userId);

		TokenPair pair = auth.refresh(refreshToken, currentVerExpected);

		addRefreshCookie(res, pair.refreshToken(), (int)rtTtlSeconds);
		return ResponseEntity.ok(new RefreshResp(pair.accessToken()));
	}

	@Operation(summary = "사용자 로그아웃", description = "토큰을 확인하여 해당 사용자를 로그아웃합니다.")
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

		auth.logout(at, userId);
		clearRefreshCookie(res);
		return ResponseEntity.noContent().build();
	}

	private void addRefreshCookie(HttpServletResponse res, String refreshToken, int maxAgeSeconds) {
		ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(RT_COOKIE_NAME, refreshToken)
			.httpOnly(true)
			.secure(true)
			.path(RT_COOKIE_PATH)
			.maxAge(Duration.ofSeconds(maxAgeSeconds))
			.sameSite("Lax");
		res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
	}

	private void clearRefreshCookie(HttpServletResponse res) {
		ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(RT_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(true)
			.path(RT_COOKIE_PATH)
			.maxAge(Duration.ZERO)
			.sameSite("Lax");
		res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
	}
}
