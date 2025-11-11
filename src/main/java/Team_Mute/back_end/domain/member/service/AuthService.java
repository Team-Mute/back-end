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

/**
 * 일반 사용자 인증 서비스 클래스
 * JWT 기반의 사용자 인증 시스템을 담당하는 핵심 비즈니스 로직
 * RTR(Refresh Token Rotation) 전략과 Redis 기반 세션 관리를 통한 보안 강화
 *
 * 주요 기능:
 * - 사용자 로그인 시 JWT Access/Refresh Token 생성
 * - Redis를 통한 세션 정보 저장 및 관리
 * - Refresh Token 재사용 감지 및 무효화 처리
 * - Token Version 검증으로 비밀번호 변경 시 기존 토큰 무효화
 * - RTR 전략으로 토큰 재발급 시마다 새로운 Refresh Token 생성
 * - 로그아웃 처리 (Access Token 블랙리스트 등록 및 세션 삭제)
 *
 * AdminAuthService와의 차이점:
 * - Audience: "user-service" (관리자는 "admin-service")
 * - 로그인 시 device, ip 정보 기록 (감사 로그)
 * - 파라미터 기반 로그인 (엔티티가 아닌 개별 파라미터)
 *
 * @author Team Mute
 * @since 1.0
 */
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

	/**
	 * 사용자 로그인 처리
	 * - JWT Access Token과 Refresh Token 생성
	 * - Redis에 세션 정보 저장 (device, ip 정보 포함)
	 * - Refresh Token JTI를 세션과 연결하여 저장
	 *
	 * 처리 흐름:
	 * 1. 새로운 세션 ID(sid) 생성
	 * 2. JWT 클레임 맵 생성 (companyId, sessionId, tokenVer, roles)
	 * 3. Access Token과 Refresh Token 생성
	 * 4. 세션 정보를 JSON으로 직렬화하여 Redis에 저장 (device, ip 포함)
	 * 5. Refresh Token JTI를 Redis에 저장 (세션과 연결)
	 * 6. TokenPair 반환 (컨트롤러에서 Access Token은 응답 바디, Refresh Token은 쿠키로 전달)
	 *
	 * 보안 고려사항:
	 * - device와 ip 정보를 세션에 기록하여 감사 로그 생성
	 * - Refresh Token TTL과 동일한 시간으로 세션 만료 설정
	 * - Refresh Token JTI를 별도로 저장하여 RTR 전략 구현
	 *
	 * @param userId 사용자 ID (문자열)
	 * @param companyId 소속 기업 ID (문자열)
	 * @param ver Token Version (비밀번호 변경 시 증가)
	 * @param roles 사용자 역할 ID (일반 사용자: 3)
	 * @param device 로그인 디바이스 정보 (예: "Android", "iOS", "Web", null 허용)
	 * @param ip 클라이언트 IP 주소 (null 허용)
	 * @return TokenPair (Access Token, Refresh Token)
	 * @throws RuntimeException 세션 저장 실패 시
	 */
	public TokenPair login(String userId, String companyId, Integer ver, Integer roles, String device, String ip) {
		// 1. 새로운 세션 ID 생성 (예: "sess-123e4567-e89b-12d3-a456-426614174000")
		String sid = IdGenerator.newSid();

		// 2. JWT 클레임 데이터 준비
		Map<String, Object> claims = new HashMap<>();
		claims.put("cid", companyId);          // 소속 기업 ID
		claims.put("sid", sid);                // 세션 ID
		claims.put("ver", ver);                // Token Version
		claims.put("roles", roles);            // 사용자 역할 ID

		// 3. Access Token 생성 (짧은 만료 시간, 예: 1시간)
		String at = jwt.createAccessToken(
			IdGenerator.newJtiAT(),    // JWT ID (예: "at-uuid")
			USER_AUDIENCE,             // Audience ("user-service")
			userId,                    // Subject (사용자 ID)
			claims                     // 커스텀 클레임
		);

		// 4. Refresh Token 생성 (긴 만료 시간, 예: 7일)
		String rt = jwt.createRefreshToken(
			IdGenerator.newJtiRT(),    // JWT ID (예: "rt-base64string")
			USER_AUDIENCE,             // Audience ("user-service")
			userId,                    // Subject (사용자 ID)
			claims                     // 커스텀 클레임
		);

		// 5. Redis에 저장할 세션 정보 준비
		Duration rtTtl = Duration.ofSeconds(props.refreshToken().ttlSeconds());
		Map<String, Object> session = Map.of(
			"userId", userId,
			"cid", companyId,
			"device", device == null ? "unknown" : device,  // 디바이스 정보 (null이면 "unknown")
			"ip", ip == null ? "0.0.0.0" : ip,              // IP 주소 (null이면 "0.0.0.0")
			"loginAt", Instant.now().toString()             // 로그인 시각 기록 (감사 로그)
		);

		// 6. 세션 정보를 JSON으로 직렬화하여 Redis에 저장
		try {
			store.saveSessionJson(
				sid,                              // 세션 ID (키)
				userId,                           // 사용자 ID (사용자별 세션 집합 관리)
				om.writeValueAsString(session),   // JSON 직렬화된 세션 정보
				rtTtl                             // TTL (Refresh Token 만료 시간과 동일)
			);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// 7. Refresh Token JTI를 Redis에 저장 (세션과 연결)
		// RTR 전략을 위해 현재 유효한 Refresh Token JTI를 별도로 저장
		String rtJti = parseJti(rt);
		store.setCurrentRtJti(sid, rtJti, rtTtl);

		// 8. TokenPair 반환 (컨트롤러에서 응답 처리)
		return new TokenPair(at, rt);
	}

	/**
	 * Refresh Token을 사용한 토큰 재발급
	 * RTR(Refresh Token Rotation) 전략 구현으로 보안 강화
	 *
	 * 처리 흐름:
	 * 1. Refresh Token 파싱 및 검증
	 * 2. Audience 검증 (사용자 토큰인지 확인)
	 * 3. Refresh Token JTI 폐기 여부 확인
	 * 4. 세션 유효성 검증 (Redis에 세션이 존재하는지)
	 * 5. Refresh Token JTI가 현재 유효한 JTI와 일치하는지 확인 (Stale Token 방지)
	 * 6. Token Version 검증 (비밀번호 변경 여부 확인)
	 * 7. 새로운 Access Token과 Refresh Token 생성 (RTR 전략)
	 * 8. 기존 Refresh Token을 블랙리스트에 등록
	 * 9. 새로운 Refresh Token JTI를 Redis에 저장
	 * 10. 새로운 TokenPair 반환
	 *
	 * 보안 기능:
	 * - Stale Refresh Token 감지 (이전 버전의 Refresh Token 재사용 방지)
	 * - Token Version 불일치 시 재로그인 요구
	 * - 세션 기반 검증으로 서버 측 토큰 무효화 가능
	 *
	 * RTR 전략:
	 * - 매 재발급마다 새로운 Refresh Token 생성
	 * - 기존 Refresh Token은 즉시 폐기 (블랙리스트 등록)
	 * - 현재 유효한 Refresh Token JTI만 Redis에 저장
	 *
	 * @param refreshToken 클라이언트가 제공한 Refresh Token (쿠키에서 추출)
	 * @param currentVerExpected 예상되는 현재 Token Version (null 허용, 검증 스킵 가능)
	 * @return TokenPair (새로운 Access Token, 새로운 Refresh Token)
	 * @throws RuntimeException 토큰 검증 실패, 세션 무효, Token Version 불일치 시
	 */
	public TokenPair refresh(String refreshToken, Integer currentVerExpected) {
		// 1. Refresh Token 파싱 및 클레임 추출
		Jws<Claims> jws = jwt.parse(refreshToken);
		Claims c = jws.getPayload();

		// 2. Audience 검증 (사용자 토큰인지 확인)
		if (!c.getAudience().contains(USER_AUDIENCE)) {
			throw new RuntimeException("유효하지 않은 토큰 타입입니다.");
		}

		// 3. Refresh Token JTI 추출 및 폐기 여부 확인
		String rtJti = c.getId();
		if (store.isRtRevoked(rtJti))
			throw new RuntimeException("revoked refresh");

		// 4. 세션 ID 추출 및 유효성 검증
		String sid = (String)c.get("sid");
		String userId = c.getSubject();
		if (sid == null || store.getSessionJson(sid) == null)
			throw new RuntimeException("no session");

		// 5. Stale Refresh Token 검증
		// Redis에 저장된 현재 유효한 Refresh Token JTI와 비교
		String currentRtJti = store.getCurrentRtJti(sid);
		if (currentRtJti == null || !currentRtJti.equals(rtJti))
			throw new RuntimeException("stale refresh");

		// 6. Token Version 검증
		// 토큰에 저장된 Token Version과 예상 버전 비교
		Integer ver = c.get("ver", Integer.class);
		if (currentVerExpected != null && !currentVerExpected.equals(ver))
			throw new RuntimeException("version changed");

		// 7. RTR 전략: 새로운 Access Token과 Refresh Token 생성
		// 기존 클레임 정보를 새 토큰에 복사
		Map<String, Object> newClaims = new HashMap<>();
		newClaims.put("cid", c.get("cid"));
		newClaims.put("sid", c.get("sid"));      // 세션 ID는 유지
		newClaims.put("ver", c.get("ver"));
		newClaims.put("roles", c.get("roles"));

		// 새로운 Access Token 생성 (새로운 JTI 부여)
		String newAT = jwt.createAccessToken(
			IdGenerator.newJtiAT(),
			USER_AUDIENCE,
			userId,
			newClaims
		);

		// 새로운 Refresh Token 생성 (새로운 JTI 부여)
		String newRtJti = IdGenerator.newJtiRT();
		String newRT = jwt.createRefreshToken(
			newRtJti,
			USER_AUDIENCE,
			userId,
			newClaims
		);

		Duration rtTtl = Duration.ofSeconds(props.refreshToken().ttlSeconds());

		// 8. 기존 Refresh Token을 블랙리스트에 등록
		store.revokeRt(rtJti, rtTtl);

		// 9. 새로운 Refresh Token JTI를 Redis에 저장 (현재 유효한 JTI 업데이트)
		store.setCurrentRtJti(sid, newRtJti, rtTtl);

		// 10. 새로운 TokenPair 반환
		return new TokenPair(newAT, newRT);
	}

	/**
	 * 사용자 로그아웃 처리
	 * - Access Token을 블랙리스트에 등록하여 즉시 무효화
	 * - Redis에서 세션 정보 삭제
	 * - Refresh Token을 블랙리스트에 등록
	 *
	 * 처리 흐름:
	 * 1. Access Token 파싱 및 JTI 추출
	 * 2. 세션 ID 추출
	 * 3. Access Token JTI를 블랙리스트에 등록 (남은 만료 시간 동안 유지)
	 * 4. Redis에서 세션 삭제
	 * 5. 현재 유효한 Refresh Token JTI 조회
	 * 6. Refresh Token을 블랙리스트에 등록
	 *
	 * 보안 고려사항:
	 * - Access Token이 아직 만료되지 않은 경우 블랙리스트에 등록하여 즉시 무효화
	 * - 세션 삭제로 Refresh Token 재사용 방지
	 * - 이미 만료된 토큰은 블랙리스트에 등록하지 않음 (메모리 절약)
	 *
	 * @param accessToken 로그아웃할 사용자의 Access Token
	 * @param userId 로그아웃할 사용자 ID (세션 삭제 시 사용)
	 */
	public void logout(String accessToken, String userId) {
		// 1. Access Token 파싱 및 클레임 추출
		Jws<Claims> jws = jwt.parse(accessToken);
		Claims c = jws.getPayload();

		// 2. Access Token JTI 추출
		String jti = c.getId();

		// 3. 세션 ID 추출
		String sid = (String)c.get("sid");

		// 4. Access Token의 남은 만료 시간 계산
		Duration ttl = Duration.between(Instant.now(), c.getExpiration().toInstant());

		// 5. Access Token이 아직 만료되지 않은 경우 블랙리스트에 등록
		if (!ttl.isNegative()) {
			store.blacklistAccessJti(jti, ttl);
		}

		// 6. Redis에서 세션 삭제
		store.deleteSession(sid, userId);

		// 7. 현재 유효한 Refresh Token JTI 조회
		String rtJti = store.getCurrentRtJti(sid);

		// 8. Refresh Token을 블랙리스트에 등록
		if (rtJti != null) {
			store.revokeRt(rtJti, ttl);
		}
	}

	/**
	 * JWT 토큰에서 JTI(JWT ID) 추출
	 * - 토큰을 파싱하여 고유 식별자 추출
	 * - 내부 헬퍼 메서드
	 *
	 * @param token 파싱할 JWT 토큰
	 * @return JTI (JWT ID)
	 */
	private String parseJti(String token) {
		return jwt.parse(token).getPayload().getId();
	}
}
