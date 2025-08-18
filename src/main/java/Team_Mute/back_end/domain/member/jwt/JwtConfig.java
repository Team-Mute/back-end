package Team_Mute.back_end.domain.member.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
	String issuer,
	String audience,
	Access accessToken,
	Refresh refreshToken,
	String secretBase64
) {
	public record Access(long ttlSeconds) {
	}

	public record Refresh(long ttlSeconds) {
	}
}
