package Team_Mute.back_end.domain.member.session;

import java.time.Duration;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 세션 및 토큰 관리 저장소 서비스 클래스
 * StringRedisTemplate을 사용하여 JWT 인증 시스템의 세션, 블랙리스트, 토큰 상태 관리
 *
 * 주요 기능:
 * - 사용자 세션 정보 저장 및 조회 (Redis String)
 * - 사용자별 세션 ID 집합 관리 (Redis Set)
 * - Access Token JTI 블랙리스트 관리 (로그아웃 시)
 * - Refresh Token JTI 폐기 목록 관리 (RTR 전략)
 * - 현재 유효한 Refresh Token JTI 추적 (Stale Token 방지)
 *
 * Redis Key 구조:
 * - session:{sid} : 세션 정보 (JSON 문자열)
 * - user-sessions:{userId} : 사용자의 세션 ID 집합 (Set)
 * - blacklist:{jti} : Access Token 블랙리스트 (로그아웃 시)
 * - revoked-rt:{jti} : 폐기된 Refresh Token 목록 (RTR)
 * - current-rt-jti:{sid} : 세션의 현재 유효한 Refresh Token JTI
 *
 * TTL(Time To Live) 관리:
 * - 모든 키는 적절한 TTL을 설정하여 메모리 효율성 확보
 * - 세션: Refresh Token 만료 시간과 동일
 * - 블랙리스트: Access Token의 남은 만료 시간
 * - 폐기 목록: Refresh Token의 남은 만료 시간
 *
 * @author Team Mute
 * @since 1.0
 */
@Service
public class SessionStore {

	private final StringRedisTemplate redis;

	public SessionStore(StringRedisTemplate redis) {
		this.redis = redis;
	}

	/**
	 * 세션 키 생성
	 * - Redis에 저장될 세션 정보의 키 생성
	 * - 형식: "session:{sid}"
	 * - 예: "session:sess-123e4567-e89b-12d3-a456-426614174000"
	 *
	 * @param sid 세션 ID
	 * @return Redis 키 문자열
	 */
	private String kSession(String sid) {
		return "session:" + sid;
	}

	/**
	 * 사용자 세션 집합 키 생성
	 * - 한 사용자가 여러 기기에서 로그인한 경우 모든 세션 ID를 추적
	 * - Redis Set 타입으로 저장
	 * - 형식: "user-sessions:{userId}"
	 * - 예: "user-sessions:12345"
	 *
	 * @param userId 사용자 ID
	 * @return Redis 키 문자열
	 */
	private String kUserSessions(String userId) {
		return "user-sessions:" + userId;
	}

	/**
	 * Access Token 블랙리스트 키 생성
	 * - 로그아웃 시 Access Token의 JTI를 블랙리스트에 등록
	 * - 아직 만료되지 않은 토큰의 즉시 무효화에 사용
	 * - 형식: "blacklist:{jti}"
	 * - 예: "blacklist:at-123e4567-e89b-12d3-a456-426614174000"
	 *
	 * @param jti JWT ID (Access Token의 고유 식별자)
	 * @return Redis 키 문자열
	 */
	private String kBlacklist(String jti) {
		return "blacklist:" + jti;
	}

	/**
	 * 폐기된 Refresh Token 키 생성
	 * - RTR(Refresh Token Rotation) 전략에서 사용
	 * - 재발급된 Refresh Token의 이전 JTI를 폐기 목록에 등록
	 * - 폐기된 토큰 재사용 시 보안 위협으로 간주
	 * - 형식: "revoked-rt:{jti}"
	 * - 예: "revoked-rt:rt-xYz123ABC..."
	 *
	 * @param jti Refresh Token의 JWT ID
	 * @return Redis 키 문자열
	 */
	private String kRevokedRt(String jti) {
		return "revoked-rt:" + jti;
	}

	/**
	 * 현재 유효한 Refresh Token JTI 키 생성
	 * - 각 세션의 현재 유효한 Refresh Token JTI를 추적
	 * - Stale Refresh Token 감지에 사용 (이전 버전 토큰 재사용 방지)
	 * - 형식: "current-rt-jti:{sid}"
	 * - 예: "current-rt-jti:sess-123e4567-e89b-12d3-a456-426614174000"
	 *
	 * @param sid 세션 ID
	 * @return Redis 키 문자열
	 */
	private String kCurrentRtJti(String sid) {
		return "current-rt-jti:" + sid;
	}

	/**
	 * 세션 정보 저장
	 * - 세션 정보를 JSON 문자열로 Redis에 저장
	 * - 사용자의 세션 ID 집합에 sid 추가 (Redis Set)
	 * - TTL 설정으로 자동 만료 처리
	 *
	 * 처리 로직:
	 * 1. session:{sid} 키에 JSON 문자열 저장 (String 타입)
	 * 2. user-sessions:{userId} Set에 sid 추가 (Set 타입)
	 * 3. user-sessions:{userId}에 TTL 설정 (세션과 동일한 만료 시간)
	 *
	 * 사용 예시:
	 * - AuthService.login(): 로그인 시 세션 생성
	 * - AdminAuthService.login(): 관리자 로그인 시 세션 생성
	 *
	 * @param sid 세션 ID
	 * @param userId 사용자 ID
	 * @param json 세션 정보 JSON 문자열 (userId, cid, device, ip, loginAt 등)
	 * @param ttl 세션 만료 시간 (Refresh Token TTL과 동일)
	 */
	public void saveSessionJson(String sid, String userId, String json, Duration ttl) {
		// 1. 세션 정보 저장 (String 타입)
		redis.opsForValue().set(kSession(sid), json, ttl);

		// 2. 사용자의 세션 ID 집합에 추가 (Set 타입)
		redis.opsForSet().add(kUserSessions(userId), sid);

		// 3. 세션 집합에 TTL 설정
		redis.expire(kUserSessions(userId), ttl);
	}

	/**
	 * 세션 정보 조회
	 * - 세션 ID로 Redis에서 세션 정보 조회
	 * - 세션이 존재하지 않거나 만료된 경우 null 반환
	 *
	 * 사용 예시:
	 * - AuthService.refresh(): 토큰 재발급 시 세션 유효성 검증
	 * - AdminAuthService.refresh(): 관리자 토큰 재발급 시 세션 검증
	 *
	 * @param sid 세션 ID
	 * @return 세션 정보 JSON 문자열 (존재하지 않으면 null)
	 */
	public String getSessionJson(String sid) {
		return redis.opsForValue().get(kSession(sid));
	}

	/**
	 * 세션 삭제
	 * - Redis에서 세션 정보 삭제
	 * - 사용자의 세션 ID 집합에서 sid 제거
	 * - 로그아웃 및 회원 탈퇴 시 사용
	 *
	 * 처리 로직:
	 * 1. session:{sid} 키 삭제
	 * 2. user-sessions:{userId} Set에서 sid 제거
	 *
	 * 사용 예시:
	 * - AuthService.logout(): 로그아웃 시 세션 삭제
	 * - UserService.deleteUser(): 회원 탈퇴 시 모든 세션 삭제
	 * - AdminService.deleteUser(): 관리자 삭제 시 모든 세션 삭제
	 *
	 * @param sid 삭제할 세션 ID
	 * @param userId 사용자 ID
	 */
	public void deleteSession(String sid, String userId) {
		// 1. 세션 정보 삭제
		redis.delete(kSession(sid));

		// 2. 사용자 세션 집합에서 제거
		redis.opsForSet().remove(kUserSessions(userId), sid);
	}

	/**
	 * 사용자의 모든 세션 ID 조회
	 * - 한 사용자가 여러 기기에서 로그인한 경우 모든 세션 ID 반환
	 * - Redis Set 타입에서 모든 멤버 조회
	 *
	 * 사용 예시:
	 * - UserService.deleteUser(): 회원 탈퇴 시 모든 세션 삭제를 위해 조회
	 * - AdminService.deleteUser(): 관리자 삭제 시 모든 세션 삭제를 위해 조회
	 * - AdminAuthService.refresh(): 재사용 공격 감지 시 모든 세션 종료
	 *
	 * @param userId 사용자 ID
	 * @return 세션 ID 집합 (Set<String>), 없으면 빈 Set 또는 null
	 */
	public Set<String> getUserSids(String userId) {
		return redis.opsForSet().members(kUserSessions(userId));
	}

	/**
	 * Access Token JTI를 블랙리스트에 등록
	 * - 로그아웃 시 아직 만료되지 않은 Access Token을 즉시 무효화
	 * - TTL 설정으로 토큰이 자연 만료되면 자동 삭제
	 * - 메모리 효율성 확보
	 *
	 * 처리 로직:
	 * 1. blacklist:{jti} 키에 "1" 값 저장
	 * 2. TTL은 Access Token의 남은 만료 시간과 동일
	 *
	 * 사용 예시:
	 * - AuthService.logout(): 로그아웃 시 Access Token 블랙리스트 등록
	 *
	 * @param jti Access Token의 JWT ID
	 * @param ttl Access Token의 남은 만료 시간
	 */
	public void blacklistAccessJti(String jti, Duration ttl) {
		redis.opsForValue().set(kBlacklist(jti), "1", ttl);
	}

	/**
	 * Access Token JTI가 블랙리스트에 있는지 확인
	 * - 로그아웃된 토큰인지 검증
	 * - JwtAuthFilter에서 매 요청마다 확인
	 *
	 * 사용 예시:
	 * - JwtAuthFilter.doFilterInternal(): 토큰 검증 시 블랙리스트 확인
	 *
	 * @param jti Access Token의 JWT ID
	 * @return 블랙리스트에 있으면 true, 없으면 false
	 */
	public boolean isBlacklisted(String jti) {
		return redis.hasKey(kBlacklist(jti));
	}

	/**
	 * Refresh Token JTI를 폐기 목록에 등록
	 * - RTR(Refresh Token Rotation) 전략에서 사용
	 * - 토큰 재발급 시 이전 Refresh Token을 폐기 목록에 등록
	 * - 폐기된 토큰 재사용 시 보안 위협으로 간주
	 *
	 * 처리 로직:
	 * 1. revoked-rt:{jti} 키에 "1" 값 저장
	 * 2. TTL은 Refresh Token의 남은 만료 시간과 동일
	 *
	 * 사용 예시:
	 * - AuthService.refresh(): 토큰 재발급 시 기존 Refresh Token 폐기
	 * - AdminAuthService.refresh(): 관리자 토큰 재발급 시 기존 토큰 폐기
	 * - UserService.deleteUser(): 회원 탈퇴 시 Refresh Token 폐기
	 *
	 * @param rtJti Refresh Token의 JWT ID
	 * @param ttl Refresh Token의 남은 만료 시간
	 */
	public void revokeRt(String rtJti, Duration ttl) {
		redis.opsForValue().set(kRevokedRt(rtJti), "1", ttl);
	}

	/**
	 * Refresh Token JTI가 폐기되었는지 확인
	 * - 폐기된 Refresh Token 재사용 방지
	 * - RTR 전략의 핵심 검증 로직
	 *
	 * 보안 시나리오:
	 * 1. 정상 흐름: 토큰 재발급 → 이전 토큰 폐기 → 새 토큰 사용
	 * 2. 공격 시나리오: 폐기된 토큰 재사용 시도 → 감지 → 모든 세션 종료
	 *
	 * 사용 예시:
	 * - AuthService.refresh(): 토큰 재발급 시 폐기 여부 확인
	 * - AdminAuthService.refresh(): 관리자 토큰 재발급 시 확인 및 공격 감지
	 *
	 * @param rtJti Refresh Token의 JWT ID
	 * @return 폐기되었으면 true, 아니면 false
	 */
	public boolean isRtRevoked(String rtJti) {
		return redis.hasKey(kRevokedRt(rtJti));
	}

	/**
	 * 현재 유효한 Refresh Token JTI 설정
	 * - 각 세션의 현재 유효한 Refresh Token JTI를 추적
	 * - Stale Refresh Token 감지에 사용
	 * - 토큰 재발급 시마다 업데이트
	 *
	 * 처리 로직:
	 * 1. current-rt-jti:{sid} 키에 rtJti 값 저장
	 * 2. TTL은 Refresh Token 만료 시간과 동일
	 *
	 * 사용 예시:
	 * - AuthService.login(): 로그인 시 초기 Refresh Token JTI 설정
	 * - AuthService.refresh(): 토큰 재발급 시 새 JTI로 업데이트
	 *
	 * @param sid 세션 ID
	 * @param rtJti 현재 유효한 Refresh Token의 JWT ID
	 * @param ttl Refresh Token 만료 시간
	 */
	public void setCurrentRtJti(String sid, String rtJti, Duration ttl) {
		redis.opsForValue().set(kCurrentRtJti(sid), rtJti, ttl);
	}

	/**
	 * 현재 유효한 Refresh Token JTI 조회
	 * - 세션의 현재 유효한 Refresh Token JTI 반환
	 * - Stale Refresh Token 감지에 사용
	 *
	 * Stale Token 검증 로직:
	 * 1. 요청된 Refresh Token의 JTI 추출
	 * 2. Redis에서 현재 유효한 JTI 조회
	 * 3. 두 값 비교: 일치하면 정상, 불일치하면 Stale Token
	 *
	 * 사용 예시:
	 * - AuthService.refresh(): 토큰 재발급 시 Stale Token 검증
	 * - AuthService.logout(): 로그아웃 시 현재 Refresh Token 폐기
	 *
	 * @param sid 세션 ID
	 * @return 현재 유효한 Refresh Token의 JWT ID (없으면 null)
	 */
	public String getCurrentRtJti(String sid) {
		return redis.opsForValue().get(kCurrentRtJti(sid));
	}
}
