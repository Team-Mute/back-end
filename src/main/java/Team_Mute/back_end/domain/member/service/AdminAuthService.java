package Team_Mute.back_end.domain.member.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import Team_Mute.back_end.domain.member.entity.Admin;
import Team_Mute.back_end.domain.member.jwt.IdGenerator;
import Team_Mute.back_end.domain.member.jwt.JwtConfig;
import Team_Mute.back_end.domain.member.jwt.JwtService;
import Team_Mute.back_end.domain.member.jwt.TokenPair;
import Team_Mute.back_end.domain.member.session.SessionStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

	private final JwtService jwt;
	private final SessionStore store;
	private final JwtConfig props;
	private final ObjectMapper om = new ObjectMapper();
	private final AdminService userService;

	private static final String ADMIN_AUDIENCE = "admin-service";

	public TokenPair login(Admin admin) {
		String sid = IdGenerator.newSid();
		String userId = admin.getAdminId().toString();
		Integer regionId;

		Map<String, Object> accessTokenClaims = new HashMap<>();
		accessTokenClaims.put("roleId", admin.getUserRole().getRoleId());
		if (admin.getAdminRegion() == null) {
			regionId = -1;
		} else {
			regionId = admin.getAdminRegion().getRegionId();
		}
		accessTokenClaims.put("regionId", regionId);
		accessTokenClaims.put("tokenVer", admin.getTokenVer());
		accessTokenClaims.put("sid", sid);

		String at = jwt.createAccessToken(IdGenerator.newJtiAT(), ADMIN_AUDIENCE, userId, accessTokenClaims);

		Map<String, Object> refreshTokenClaims = new HashMap<>();
		refreshTokenClaims.put("regionId", regionId);
		refreshTokenClaims.put("tokenVer", admin.getTokenVer());
		refreshTokenClaims.put("sid", sid);

		String rt = jwt.createRefreshToken(IdGenerator.newJtiRT(), ADMIN_AUDIENCE, userId, refreshTokenClaims);

		Duration rtTtl = Duration.ofSeconds(props.refreshToken().ttlSeconds());
		Map<String, Object> session = Map.of(
			"userId", userId,
			"roleId", admin.getUserRole().getRoleId(),
			"loginAt", Instant.now().toString()
		);

		try {
			store.saveSessionJson(sid, userId, om.writeValueAsString(session), rtTtl);
		} catch (Exception e) {
			throw new RuntimeException("세션 저장에 실패했습니다.", e);
		}

		return new TokenPair(at, rt);
	}

	public TokenPair refresh(String refreshToken) {
		String rtJti = jwt.parse(refreshToken).getPayload().getId();
		if (store.isRtRevoked(rtJti)) {
			Jws<Claims> jws = jwt.parse(refreshToken);
			String userId = jws.getPayload().getSubject();
			log.warn("폐기된 리프레시 토큰 재사용 시도 감지! 사용자 {}의 모든 세션을 종료합니다.", userId);

			Set<String> allSessionIds = store.getUserSids(userId);
			if (allSessionIds != null) {
				allSessionIds.forEach(sid -> store.deleteSession(sid, userId));
			}
			throw new RuntimeException("비정상적인 접근이 감지되어 모든 세션이 종료되었습니다. 다시 로그인해주세요.");
		}

		Jws<Claims> jws = jwt.parse(refreshToken);
		Claims c = jws.getPayload();

		if (!c.getAudience().contains(ADMIN_AUDIENCE)) {
			throw new RuntimeException("유효하지 않은 토큰 타입입니다.");
		}

		String userId = c.getSubject();
		if (userId == null)
			throw new RuntimeException("사용자 ID가 없습니다.");

		String sid = (String)c.get("sid");
		if (sid == null || store.getSessionJson(sid) == null) {
			throw new RuntimeException("만료되었거나 유효하지 않은 세션입니다.");
		}

		Integer tokenVerInToken = c.get("tokenVer", Integer.class);
		Integer currentVerInDb = userService.getTokenVer(Long.valueOf(userId));
		if (tokenVerInToken == null || !tokenVerInToken.equals(currentVerInDb)) {
			throw new RuntimeException("토큰 버전이 변경되어 재로그인이 필요합니다.");
		}

		// 3. [회전] 새로운 액세스 토큰과 리프레시 토큰을 모두 생성
		Map<String, Object> newAccessTokenClaims = new HashMap<>();
		newAccessTokenClaims.put("roleId", c.get("roleId", Integer.class));
		newAccessTokenClaims.put("regionId", c.get("regionId", Integer.class));
		newAccessTokenClaims.put("tokenVer", tokenVerInToken);
		newAccessTokenClaims.put("sid", sid);

		String newAT = jwt.createAccessToken(IdGenerator.newJtiAT(), ADMIN_AUDIENCE, userId, newAccessTokenClaims);

		Map<String, Object> newRefreshTokenClaims = new HashMap<>();
		newRefreshTokenClaims.put("regionId", c.get("regionId", Integer.class));
		newRefreshTokenClaims.put("tokenVer", tokenVerInToken);
		newRefreshTokenClaims.put("sid", sid);

		String newRT = jwt.createRefreshToken(IdGenerator.newJtiRT(), ADMIN_AUDIENCE, userId, newRefreshTokenClaims);

		Duration remainingTtl = Duration.between(Instant.now(), c.getExpiration().toInstant());
		if (!remainingTtl.isNegative()) {
			store.revokeRt(rtJti, remainingTtl);
		}

		return new TokenPair(newAT, newRT);
	}
}
