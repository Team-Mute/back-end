package Team_Mute.back_end.domain.member.jwt;

/**
 * JWT 토큰 클레임 정보를 담는 불변 데이터 클래스 (Java Record)
 * JWT 페이로드에 포함될 사용자 정보를 구조화
 * AuthService에서 로그인 시 사용자 정보를 TokenClaims로 변환하여 JWT 생성
 * 불변 객체로 스레드 안전성 보장
 *
 * @author Team Mute
 * @since 1.0
 */
public record TokenClaims(
	/**
	 * 사용자 ID (Subject)
	 * - JWT 표준 클레임 "sub"에 해당
	 * - 토큰 주체를 식별하는 고유 ID
	 * - String 타입으로 User.userId 또는 Admin.adminId를 문자열로 변환
	 */
	String sub,

	/**
	 * 회사 ID (Company ID)
	 * - 사용자의 소속 기업 식별자
	 * - 커스텀 클레임 "cid"
	 * - String 타입으로 UserCompany.companyId를 문자열로 변환
	 * - 기업별 권한 분리 및 통계에 사용
	 */
	String cid,

	/**
	 * 세션 ID (Session ID)
	 * - 로그인 세션을 추적하는 고유 식별자
	 * - 커스텀 클레임 "sid"
	 * - IdGenerator.newSid()로 생성된 세션 ID
	 * - Redis에 저장된 세션과 매핑
	 * - 예: "sess-123e4567-e89b-12d3-a456-426614174000"
	 */
	String sid,

	/**
	 * 토큰 버전 (Token Version)
	 * - 커스텀 클레임 "ver"
	 * - User.tokenVer 또는 Admin.tokenVer 값
	 * - 비밀번호 변경 시 증가하여 기존 토큰 무효화
	 * - 토큰 재발급 시 현재 버전과 비교하여 검증
	 * - 버전이 다르면 인증 실패 처리
	 */
	Integer ver,

	/**
	 * 역할 ID (Role ID)
	 * - 커스텀 클레임 "roles"
	 * - UserRole.roleId 값
	 * - 0: 마스터 관리자
	 * - 1: 2차 승인자
	 * - 2: 1차 승인자
	 * - 3: 일반 사용자
	 * - 권한별 접근 제어 및 UI 분기 처리에 사용
	 */
	Integer roles
) {
	/**
	 * TokenClaims 정적 팩토리 메서드
	 * - 파라미터를 받아 TokenClaims 인스턴스 생성
	 * - 가독성 향상을 위한 명시적 생성 메서드
	 *
	 * @param sub 사용자 ID
	 * @param cid 회사 ID
	 * @param sid 세션 ID
	 * @param ver 토큰 버전
	 * @param roles 역할 ID
	 * @return 생성된 TokenClaims 인스턴스
	 */
	public static TokenClaims of(String sub, String cid, String sid, Integer ver, Integer roles) {
		return new TokenClaims(sub, cid, sid, ver, roles);
	}
}
