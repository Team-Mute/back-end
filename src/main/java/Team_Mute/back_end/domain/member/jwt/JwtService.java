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

/**
 * JWT 토큰 생성 및 검증 서비스 클래스
 * JJWT(Java JWT) 라이브러리를 사용하여 JWT 토큰의 생성과 파싱 기능 제공
 * HMAC-SHA256 알고리즘으로 토큰 서명
 * Access Token과 Refresh Token 모두 이 서비스를 통해 생성
 *
 * @author Team Mute
 * @since 1.0
 */
@Service
public class JwtService {
	/**
	 * JWT 설정 정보
	 * - application.properties에서 로드된 설정
	 * - 발급자, 대상, 만료 시간, 비밀 키 등 포함
	 */
	private final JwtConfig props;

	/**
	 * JWT 서명에 사용할 비밀 키
	 * - HMAC-SHA256 알고리즘용 SecretKey
	 * - Base64 디코딩된 비밀 키로 생성
	 * - 불변 객체로 스레드 안전
	 */
	private final SecretKey key;

	/**
	 * JwtService 생성자
	 * - JwtConfig를 주입받아 초기화
	 * - Base64로 인코딩된 비밀 키를 디코딩하여 SecretKey 객체 생성
	 * - HMAC-SHA256에 적합한 키 길이 검증 (최소 256비트)
	 *
	 * @param props JWT 설정 정보
	 */
	public JwtService(JwtConfig props) {
		this.props = props;
		byte[] secret = Base64.getDecoder().decode(props.secretBase64());
		this.key = Keys.hmacShaKeyFor(secret);
	}

	/**
	 * Access Token 생성
	 * - JWT 표준 클레임과 커스텀 클레임을 포함하여 Access Token 생성
	 * - HMAC-SHA256 알고리즘으로 서명
	 * - 짧은 만료 시간으로 보안 강화
	 * - API 요청 시 Authorization 헤더에 "Bearer {token}" 형식으로 포함
	 *
	 * @param jti JWT ID (고유 식별자, IdGenerator.newJtiAT()로 생성)
	 * @param audience 토큰 대상 (일반적으로 "user" 또는 "admin")
	 * @param subject 토큰 주체 (사용자 ID)
	 * @param claims 추가 커스텀 클레임 (companyId, sessionId, version, roles 등)
	 * @return 생성된 Access Token 문자열 (JWT 형식: header.payload.signature)
	 */
	public String createAccessToken(String jti, String audience, String subject, Map<String, Object> claims) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(props.accessToken().ttlSeconds());
		JwtBuilder builder = Jwts.builder()
			.id(jti)                                    // JWT ID (jti 클레임)
			.issuer(props.issuer())                     // 발급자 (iss 클레임)
			.audience().add(audience).and()             // 대상 (aud 클레임)
			.subject(subject)                           // 주체 (sub 클레임, 사용자 ID)
			.issuedAt(Date.from(now))                   // 발급 시각 (iat 클레임)
			.expiration(Date.from(exp));                // 만료 시각 (exp 클레임)

		claims.forEach(builder::claim);

		return builder.signWith(key, Jwts.SIG.HS256).compact();
	}

	/**
	 * Refresh Token 생성
	 * - Access Token 재발급에 사용되는 Refresh Token 생성
	 * - HMAC-SHA256 알고리즘으로 서명
	 * - 긴 만료 시간 설정
	 * - HttpOnly 쿠키로 저장하여 XSS 공격으로부터 보호
	 * - RTR(Refresh Token Rotation) 전략으로 매 재발급 시 새로운 토큰 생성
	 *
	 * @param jti JWT ID (고유 식별자, IdGenerator.newJtiRT()로 생성)
	 * @param audience 토큰 대상 (일반적으로 "user" 또는 "admin")
	 * @param subject 토큰 주체 (사용자 ID)
	 * @param claims 추가 커스텀 클레임 (필요시 최소한의 정보만 포함)
	 * @return 생성된 Refresh Token 문자열 (JWT 형식: header.payload.signature)
	 */
	public String createRefreshToken(String jti, String audience, String subject, Map<String, Object> claims) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(props.refreshToken().ttlSeconds());
		JwtBuilder builder = Jwts.builder()
			.id(jti)                                    // JWT ID (jti 클레임)
			.issuer(props.issuer())                     // 발급자 (iss 클레임)
			.audience().add(audience).and()             // 대상 (aud 클레임)
			.subject(subject)                           // 주체 (sub 클레임, 사용자 ID)
			.issuedAt(Date.from(now))                   // 발급 시각 (iat 클레임)
			.expiration(Date.from(exp));                // 만료 시각 (exp 클레임)

		claims.forEach(builder::claim);

		return builder.signWith(key, Jwts.SIG.HS256).compact();
	}

	/**
	 * JWT 토큰 파싱 및 검증
	 * - JWT 토큰을 파싱하고 서명을 검증
	 * - 서명 검증 실패, 만료, 발급자 불일치 시 예외 발생
	 * - JwtAuthFilter에서 요청의 Authorization 헤더에서 토큰 추출 후 이 메서드로 검증
	 *
	 * 검증 항목:
	 * 1. 서명 검증 (HMAC-SHA256)
	 * 2. 발급자(issuer) 일치 여부
	 * 3. 만료 시간 확인
	 * 4. 토큰 구조 유효성
	 *
	 * 예외:
	 * - io.jsonwebtoken.ExpiredJwtException: 토큰 만료
	 * - io.jsonwebtoken.SignatureException: 서명 검증 실패
	 * - io.jsonwebtoken.MalformedJwtException: 잘못된 토큰 형식
	 * - io.jsonwebtoken.IncorrectClaimException: 발급자 불일치
	 *
	 * @param token 파싱할 JWT 토큰 문자열
	 * @return 파싱된 JWT 객체 (Jws<Claims>), 클레임 정보 접근 가능
	 * @throws io.jsonwebtoken.JwtException 토큰 검증 실패 시
	 */
	public Jws<Claims> parse(String token) {
		return Jwts.parser()
			.verifyWith(key)                            // 서명 검증 키 설정
			.requireIssuer(props.issuer())              // 발급자 일치 여부 확인
			.build()
			.parseSignedClaims(token);                  // 토큰 파싱 및 검증
	}
}
