package Team_Mute.back_end.domain.member.session;

import java.time.Duration;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionStore {
	private final StringRedisTemplate redis;

	public SessionStore(StringRedisTemplate redis) {
		this.redis = redis;
	}

	private String kSession(String sid) {
		return "session:" + sid;
	}

	private String kUserSessions(String userId) {
		return "user-sessions:" + userId;
	}

	private String kBlacklist(String jti) {
		return "blacklist:" + jti;
	}

	private String kRevokedRt(String jti) {
		return "revoked-rt:" + jti;
	}

	private String kCurrentRtJti(String sid) {
		return "current-rt-jti:" + sid;
	}

	public void saveSessionJson(String sid, String userId, String json, Duration ttl) {
		redis.opsForValue().set(kSession(sid), json, ttl);
		redis.opsForSet().add(kUserSessions(userId), sid);
		redis.expire(kUserSessions(userId), ttl);
	}

	public String getSessionJson(String sid) {
		return redis.opsForValue().get(kSession(sid));
	}

	public void deleteSession(String sid, String userId) {
		redis.delete(kSession(sid));
		redis.opsForSet().remove(kUserSessions(userId), sid);
	}

	public Set<String> getUserSids(String userId) {
		return redis.opsForSet().members(kUserSessions(userId));
	}

	public void blacklistAccessJti(String jti, Duration ttl) {
		redis.opsForValue().set(kBlacklist(jti), "1", ttl);
	}

	public boolean isBlacklisted(String jti) {
		return redis.hasKey(kBlacklist(jti));
	}

	public void revokeRt(String rtJti, Duration ttl) {
		redis.opsForValue().set(kRevokedRt(rtJti), "1", ttl);
	}

	public boolean isRtRevoked(String rtJti) {
		return redis.hasKey(kRevokedRt(rtJti));
	}

	public void setCurrentRtJti(String sid, String rtJti, Duration ttl) {
		redis.opsForValue().set(kCurrentRtJti(sid), rtJti, ttl);
	}

	public String getCurrentRtJti(String sid) {
		return redis.opsForValue().get(kCurrentRtJti(sid));
	}
}
