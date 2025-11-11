package Team_Mute.back_end.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 관리자 정보 조회 응답 DTO
 * 관리자가 본인의 계정 정보를 조회할 때 클라이언트에 반환되는 데이터
 * AdminController의 getAdminInfo API 응답으로 사용
 * 민감한 정보(비밀번호)는 제외하고 필요한 정보만 포함
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class AdminInfoResponse {
	/**
	 * 관리자 역할 ID
	 * - 0: 마스터 관리자 (최고 권한)
	 * - 1: 2차 승인자 (최종 승인)
	 * - 2: 1차 승인자 (지역별 예약 1차 검토)
	 * - 프론트엔드에서 권한별 UI 분기 처리에 사용
	 */
	private Integer roleId;

	/**
	 * 담당 지역명
	 * - 1차 또는 2차 승인자의 담당 지역
	 * - 마스터 관리자는 null 가능
	 * - 예약 승인 시 담당 지역 필터링에 사용
	 */
	private String regionName;

	/**
	 * 관리자 이메일 주소
	 * - 로그인 ID로 사용되는 이메일
	 * - 계정 식별자
	 */
	private String userEmail;

	/**
	 * 관리자 이름
	 * - 실명
	 * - 시스템 내 표시 이름
	 */
	private String userName;

	/**
	 * 관리자 전화번호
	 * - 연락 가능한 전화번호
	 * - 긴급 상황 시 연락용
	 */
	private String userPhone;
}
