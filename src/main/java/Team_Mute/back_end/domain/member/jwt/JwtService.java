package Team_Mute.back_end.domain.member.jwt;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	private final JwtConfig props;
	private final SecretKey key;

	public JwtService(JwtConfig props) {
		this.props = props;
		byte[] secret = Base64.getDecoder().decode(props.secretBase64());
		this.key = Keys.hmacShaKeyFor(secret);
	}

	public String createAccessToken(String jti, String audience, String subject, Map<String, Object> claims) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(props.accessToken().ttlSeconds());
		JwtBuilder builder = Jwts.builder()
			.id(jti)
			.issuer(props.issuer())
			.audience().add(audience).and()
			.subject(subject)
			.issuedAt(Date.from(now))
			.expiration(Date.from(exp));

		claims.forEach(builder::claim);

		return builder.signWith(key, Jwts.SIG.HS256).compact();
	}

	public String createRefreshToken(String jti, String audience, String subject, Map<String, Object> claims) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(props.refreshToken().ttlSeconds());
		JwtBuilder builder = Jwts.builder()
			.id(jti)
			.issuer(props.issuer())
			.audience().add(audience).and()
			.subject(subject)
			.issuedAt(Date.from(now))
			.expiration(Date.from(exp));

		claims.forEach(builder::claim);

		return builder.signWith(key, Jwts.SIG.HS256).compact();
	}

	public Jws<Claims> parse(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.requireIssuer(props.issuer())
			.build()
			.parseSignedClaims(token);
	}
}
