package Team_Mute.back_end.domain.member.jwt;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public final class IdGenerator {
	private static final SecureRandom RNG = new SecureRandom();

	public static String newSid() {
		return "sess-" + UUID.randomUUID();
	}

	public static String newJtiAT() {
		return "at-" + UUID.randomUUID();
	}

	public static String newJtiRT() {
		byte[] buf = new byte[32]; // 256bit
		RNG.nextBytes(buf);
		return "rt-" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	}
}
