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

/**
 * 관리자 인증 서비스 클래스
 * JWT 기반의 관리자 인증 시스템을 담당하는 핵심 비즈니스 로직
 * RTR(Refresh Token Rotation) 전략과 Redis 기반 세션 관리를 통한 보안 강화
 *
 * 주요 기능:
 * - 관리자 로그인 시 JWT Access/Refresh Token 생성
 * - Redis를 통한 세션 정보 저장 및 관리
 * - Refresh Token 재사용 감지 및 모든 세션 종료 (보안 위협 대응)
 * - Token Version 검증으로 비밀번호 변경 시 기존 토큰 무효화
 * - RTR 전략으로 토큰 재발급 시마다 새로운 Refresh Token 생성
 *
 * @author Team Mute
 * @since 1.0
 */
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

	/**
	 * 관리자 로그인 처리
	 * - JWT Access Token과 Refresh Token 생성
	 * - Redis에 세션 정보 저장
	 * - 관리자 역할(roleId)과 담당 지역(regionId) 정보를 JWT 클레임에 포함
	 *
	 * 처리 흐름:
	 * 1. 새로운 세션 ID(sid) 생성
	 * 2. 관리자 정보에서 roleId, regionId, tokenVer 추출
	 * 3. JWT 클레임 맵 생성 및 데이터 설정
	 * 4. Access Token과 Refresh Token 생성
	 * 5. 세션 정보를 JSON으로 직렬화하여 Redis에 저장
	 * 6. TokenPair 반환 (컨트롤러에서 Access Token은 응답 바디, Refresh Token은 쿠키로 전달)
	 *
	 * 보안 고려사항:
	 * - adminRegion이 null인 경우 regionId를 -1로 설정 (마스터 관리자, 2차 승인자)
	 * - 세션 정보에 로그인 시각 기록 (감사 로그)
	 * - Refresh Token TTL과 동일한 시간으로 세션 만료 설정
	 *
	 * @param admin 로그인한 관리자 엔티티 (AdminAuthController에서 전달)
	 * @return TokenPair (Access Token, Refresh Token)
	 * @throws RuntimeException 세션 저장 실패 시
	 */
	public TokenPair login(Admin admin) {
		// 1. 새로운 세션 ID 생성 (예: "sess-123e4567-e89b-12d3-a456-426614174000")
		String sid = IdGenerator.newSid();

		// 2. 관리자 ID를 문자열로 변환 (JWT subject로 사용)
		String userId = admin.getAdminId().toString();

		// 3. 담당 지역 ID 추출 (null인 경우 -1로 설정)
		Integer regionId;

		// 4. JWT 클레임 데이터 준비
		Map<String, Object> TokenClaims = new HashMap<>();

		// 역할 ID 추가 (0: 마스터 관리자, 1: 1차 승인자, 2: 2차 승인자)
		TokenClaims.put("roleId", admin.getUserRole().getRoleId());

		// adminRegion이 null인 경우 처리 (마스터 관리자는 담당 지역 없음)
		if (admin.getAdminRegion() == null) {
			regionId = -1;
		} else {
			regionId = admin.getAdminRegion().getRegionId();
		}
		TokenClaims.put("regionId", regionId);

		// Token Version 추가 (비밀번호 변경 시 증가)
		TokenClaims.put("tokenVer", admin.getTokenVer());

		// 세션 ID 추가 (Redis 세션과 연결)
		TokenClaims.put("sid", sid);

		// 5. Access Token 생성 (짧은 만료 시간, 예: 1시간)
		String at = jwt.createAccessToken(
			IdGenerator.newJtiAT(),    // JWT ID (예: "at-uuid")
			ADMIN_AUDIENCE,            // Audience ("admin-service")
			userId,                    // Subject (관리자 ID)
			TokenClaims                // 커스텀 클레임
		);

		// 6. Refresh Token 생성 (긴 만료 시간, 예: 7일)
		String rt = jwt.createRefreshToken(
			IdGenerator.newJtiRT(),    // JWT ID (예: "rt-base64string")
			ADMIN_AUDIENCE,            // Audience ("admin-service")
			userId,                    // Subject (관리자 ID)
			TokenClaims                // 커스텀 클레임
		);

		// 7. Redis에 저장할 세션 정보 준비
		Duration rtTtl = Duration.ofSeconds(props.refreshToken().ttlSeconds());
		Map<String, Object> session = Map.of(
			"userId", userId,
			"roleId", admin.getUserRole().getRoleId(),
			"loginAt", Instant.now().toString()  // 로그인 시각 기록 (감사 로그)
		);

		// 8. 세션 정보를 JSON으로 직렬화하여 Redis에 저장
		try {
			store.saveSessionJson(
				sid,                              // 세션 ID (키)
				userId,                           // 사용자 ID (사용자별 세션 집합 관리)
				om.writeValueAsString(session),   // JSON 직렬화된 세션 정보
				rtTtl                             // TTL (Refresh Token 만료 시간과 동일)
			);
		} catch (Exception e) {
			throw new RuntimeException("세션 저장에 실패했습니다.", e);
		}

		// 9. TokenPair 반환 (컨트롤러에서 응답 처리)
		return new TokenPair(at, rt);
	}

	/**
	 * Refresh Token을 사용한 토큰 재발급
	 * RTR(Refresh Token Rotation) 전략 구현으로 보안 강화
	 *
	 * 처리 흐름:
	 * 1. Refresh Token 폐기 여부 확인 (재사용 공격 감지)
	 * 2. Refresh Token 파싱 및 검증
	 * 3. Audience 검증 (관리자 토큰인지 확인)
	 * 4. 세션 유효성 검증 (Redis에 세션이 존재하는지)
	 * 5. Token Version 검증 (비밀번호 변경 여부 확인)
	 * 6. 새로운 Access Token과 Refresh Token 생성 (RTR 전략)
	 * 7. 기존 Refresh Token을 블랙리스트에 등록
	 * 8. 새로운 TokenPair 반환
	 *
	 * 보안 기능:
	 * - Refresh Token 재사용 감지 시 모든 세션 강제 종료
	 * - Token Version 불일치 시 재로그인 요구
	 * - 세션 기반 검증으로 서버 측 토큰 무효화 가능
	 *
	 * RTR 전략:
	 * - 매 재발급마다 새로운 Refresh Token 생성
	 * - 기존 Refresh Token은 즉시 폐기 (블랙리스트 등록)
	 * - 폐기된 토큰 재사용 시 보안 위협으로 간주하여 모든 세션 종료
	 *
	 * @param refreshToken 클라이언트가 제공한 Refresh Token (쿠키에서 추출)
	 * @return TokenPair (새로운 Access Token, 새로운 Refresh Token)
	 * @throws RuntimeException 토큰 검증 실패, 세션 무효, Token Version 불일치 시
	 */
	public TokenPair refresh(String refreshToken) {
		// 1. Refresh Token의 JTI 추출
		String rtJti = jwt.parse(refreshToken).getPayload().getId();

		// 2. Refresh Token 재사용 공격 감지
		// Redis 블랙리스트에 이미 폐기된 토큰이 있는지 확인
		if (store.isRtRevoked(rtJti)) {
			// 재사용된 토큰으로부터 사용자 ID 추출
			Jws<Claims> jws = jwt.parse(refreshToken);
			String userId = jws.getPayload().getSubject();

			// 보안 위협 감지 로그 기록
			log.warn("폐기된 리프레시 토큰 재사용 시도 감지! 사용자 {}의 모든 세션을 종료합니다.", userId);

			// 해당 사용자의 모든 세션 ID 조회 및 삭제 (보안 조치)
			Set<String> allSessionIds = store.getUserSids(userId);
			if (allSessionIds != null) {
				allSessionIds.forEach(sid -> store.deleteSession(sid, userId));
			}

			// 클라이언트에 재로그인 요구
			throw new RuntimeException("비정상적인 접근이 감지되어 모든 세션이 종료되었습니다. 다시 로그인해주세요.");
		}

		// 3. Refresh Token 파싱 및 클레임 추출
		Jws<Claims> jws = jwt.parse(refreshToken);
		Claims c = jws.getPayload();

		// 4. Audience 검증 (관리자 토큰인지 확인)
		if (!c.getAudience().contains(ADMIN_AUDIENCE)) {
			throw new RuntimeException("유효하지 않은 토큰 타입입니다.");
		}

		// 5. Subject(사용자 ID) 추출 및 검증
		String userId = c.getSubject();
		if (userId == null) {
			throw new RuntimeException("사용자 ID가 없습니다.");
		}

		// 6. 세션 유효성 검증
		// 세션 ID 추출
		String sid = (String)c.get("sid");
		// Redis에서 세션 정보 조회
		if (sid == null || store.getSessionJson(sid) == null) {
			throw new RuntimeException("만료되었거나 유효하지 않은 세션입니다.");
		}

		// 7. Token Version 검증
		// 토큰에 저장된 Token Version 추출
		Integer tokenVerInToken = c.get("tokenVer", Integer.class);
		// 데이터베이스에서 현재 Token Version 조회
		Integer currentVerInDb = userService.getTokenVer(Long.valueOf(userId));
		// Version 비교 (불일치 시 비밀번호가 변경된 것으로 간주)
		if (tokenVerInToken == null || !tokenVerInToken.equals(currentVerInDb)) {
			throw new RuntimeException("토큰 버전이 변경되어 재로그인이 필요합니다.");
		}

		// 8. RTR 전략: 새로운 Access Token과 Refresh Token 생성
		// 기존 클레임 정보를 새 토큰에 복사
		Map<String, Object> newTokenClaims = new HashMap<>();
		newTokenClaims.put("roleId", c.get("roleId", Integer.class));
		newTokenClaims.put("regionId", c.get("regionId", Integer.class));
		newTokenClaims.put("tokenVer", tokenVerInToken);
		newTokenClaims.put("sid", sid);  // 세션 ID는 유지

		// 새로운 Access Token 생성 (새로운 JTI 부여)
		String newAT = jwt.createAccessToken(
			IdGenerator.newJtiAT(),
			ADMIN_AUDIENCE,
			userId,
			newTokenClaims
		);

		// 새로운 Refresh Token 생성 (새로운 JTI 부여)
		String newRT = jwt.createRefreshToken(
			IdGenerator.newJtiRT(),
			ADMIN_AUDIENCE,
			userId,
			newTokenClaims
		);

		// 9. 기존 Refresh Token을 블랙리스트에 등록
		// 남은 만료 시간 계산
		Duration remainingTtl = Duration.between(Instant.now(), c.getExpiration().toInstant());
		// 만료되지 않은 경우에만 블랙리스트에 추가
		if (!remainingTtl.isNegative()) {
			store.revokeRt(rtJti, remainingTtl);
		}

		// 10. 새로운 TokenPair 반환
		return new TokenPair(newAT, newRT);
	}
}
