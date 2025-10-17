package Team_Mute.back_end.domain.member.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import Team_Mute.back_end.domain.member.jwt.IdGenerator;
import Team_Mute.back_end.domain.member.jwt.JwtConfig;
import Team_Mute.back_end.domain.member.jwt.JwtService;
import Team_Mute.back_end.domain.member.jwt.TokenPair;
import Team_Mute.back_end.domain.member.session.SessionStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@Service
public class AuthService {

	private final JwtService jwt;
	private final SessionStore store;
	private final JwtConfig props;
	private final ObjectMapper om = new ObjectMapper();

	private static final String USER_AUDIENCE = "user-service";

	public AuthService(JwtService jwt, SessionStore store, JwtConfig props) {
		this.jwt = jwt;
		this.store = store;
		this.props = props;
	}

	public TokenPair login(String userId, String companyId, Integer ver, Integer roles, String device, String ip) {
		String sid = IdGenerator.newSid();

		Map<String, Object> claims = new HashMap<>();
		claims.put("cid", companyId);
		claims.put("sid", sid);
		claims.put("ver", ver);
		claims.put("roles", roles);

		String at = jwt.createAccessToken(IdGenerator.newJtiAT(), USER_AUDIENCE, userId, claims);
		String rt = jwt.createRefreshToken(IdGenerator.newJtiRT(), USER_AUDIENCE, userId, claims);

		Duration rtTtl = Duration.ofSeconds(props.refreshToken().ttlSeconds());
		Map<String, Object> session = Map.of(
			"userId", userId, "cid", companyId,
			"device", device == null ? "unknown" : device,
			"ip", ip == null ? "0.0.0.0" : ip,
			"loginAt", Instant.now().toString()
		);
		try {
			store.saveSessionJson(sid, userId, om.writeValueAsString(session), rtTtl);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		String rtJti = parseJti(rt);
		store.setCurrentRtJti(sid, rtJti, rtTtl);

		return new TokenPair(at, rt);
	}

	public TokenPair refresh(String refreshToken, Integer currentVerExpected) {
		Jws<Claims> jws = jwt.parse(refreshToken);
		Claims c = jws.getPayload();

		if (!c.getAudience().contains(USER_AUDIENCE)) {
			throw new RuntimeException("유효하지 않은 토큰 타입입니다.");
		}

		String rtJti = c.getId();
		if (store.isRtRevoked(rtJti))
			throw new RuntimeException("revoked refresh");

		String sid = (String)c.get("sid");
		String userId = c.getSubject();
		if (sid == null || store.getSessionJson(sid) == null)
			throw new RuntimeException("no session");

		String currentRtJti = store.getCurrentRtJti(sid);
		if (currentRtJti == null || !currentRtJti.equals(rtJti))
			throw new RuntimeException("stale refresh");

		Integer ver = c.get("ver", Integer.class);
		if (currentVerExpected != null && !currentVerExpected.equals(ver))
			throw new RuntimeException("version changed");

		Map<String, Object> newClaims = new HashMap<>();
		newClaims.put("cid", c.get("cid"));
		newClaims.put("sid", c.get("sid"));
		newClaims.put("ver", c.get("ver"));
		newClaims.put("roles", c.get("roles"));

		String newAT = jwt.createAccessToken(IdGenerator.newJtiAT(), USER_AUDIENCE, userId, newClaims);
		String newRtJti = IdGenerator.newJtiRT();
		String newRT = jwt.createRefreshToken(newRtJti, USER_AUDIENCE, userId, newClaims);
		Duration rtTtl = Duration.ofSeconds(props.refreshToken().ttlSeconds());

		store.revokeRt(rtJti, rtTtl);
		store.setCurrentRtJti(sid, newRtJti, rtTtl);

		return new TokenPair(newAT, newRT);
	}

	public void logout(String accessToken, String userId) {
		Jws<Claims> jws = jwt.parse(accessToken);
		Claims c = jws.getPayload();
		String jti = c.getId();
		String sid = (String)c.get("sid");

		Duration ttl = Duration.between(Instant.now(), c.getExpiration().toInstant());
		if (!ttl.isNegative()) {
			store.blacklistAccessJti(jti, ttl);
		}

		store.deleteSession(sid, userId);
		String rtJti = store.getCurrentRtJti(sid);
		if (rtJti != null) {
			store.revokeRt(rtJti, ttl);
		}
	}

	private String parseJti(String token) {
		return jwt.parse(token).getPayload().getId();
	}
}
