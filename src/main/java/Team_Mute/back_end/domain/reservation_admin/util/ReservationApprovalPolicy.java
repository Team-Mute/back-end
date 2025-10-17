package Team_Mute.back_end.domain.reservation_admin.util;

import Team_Mute.back_end.global.constants.AdminRoleEnum;
import Team_Mute.back_end.global.constants.ReservationStatusEnum;

import org.springframework.stereotype.Component;

/**
 * 예약 승인 및 반려 가능 여부를 판단하는 정책 유틸리티 클래스
 * 관리자의 역할(roleId)과 예약 정보(regionId, statusName)를 기반으로 권한을 확인
 */
@Component
public final class ReservationApprovalPolicy {

	private ReservationApprovalPolicy() {
	} // 인스턴스화 방지


	/**
	 * 특정 관리자 역할과 예약 상태/지역을 기준으로 해당 예약의 [승인 가능 여부]를 판단
	 * (리스트 체크박스 및 승인 버튼 활성화 기준)
	 *
	 * @param reservationRegionId 예약된 공간의 지역 ID
	 * @param adminRegionId       관리자의 담당 지역 ID
	 * @param roleId              관리자의 역할 ID
	 * @param statusName          현재 예약 상태 이름 (String)
	 * @return 승인 권한 및 조건이 충족되면 true
	 */
	public static boolean isApprovableFor(Integer reservationRegionId, Integer adminRegionId, Integer roleId, String statusName) {

		if (AdminRoleEnum.ROLE_FIRST_APPROVER.getId().equals(roleId)) {
			// 1차 승인자 → 1차 승인 대기만 체크 가능, 담당 지역만 체크 가능
			return ReservationStatusEnum.WAITING_FIRST_APPROVAL.getDescription().equals(statusName) && reservationRegionId.equals(adminRegionId);
		} else if (AdminRoleEnum.ROLE_SECOND_APPROVER.getId().equals(roleId)) {
			// 2차 승인자 → 1차 승인 대기, 2차 승인 대기 모두 체크 가능
			return ReservationStatusEnum.WAITING_FIRST_APPROVAL.getDescription().equals(statusName) || ReservationStatusEnum.WAITING_SECOND_APPROVAL.getDescription().equals(statusName);
		}
		return false;
	}

	/**
	 * 특정 관리자 역할과 예약 상태/지역을 기준으로 해당 예약의 [반려 가능 여부]를 판단
	 * (반려 버튼 활성화 기준)
	 *
	 * @param reservationRegionId 예약된 공간의 지역 ID
	 * @param adminRegionId       관리자의 담당 지역 ID
	 * @param roleId              관리자의 역할 ID
	 * @param statusName          현재 예약 상태 이름 (String)
	 * @return 반려 권한 및 조건이 충족되면 true
	 */
	public static boolean isRejectableFor(Integer reservationRegionId, Integer adminRegionId, Integer roleId, String statusName) {
		if (AdminRoleEnum.ROLE_FIRST_APPROVER.getId().equals(roleId)) {
			// 1차 승인자 -> 1차 승인 대기일 때만 반려 가능, 담당 지역만 반려 가능
			return ReservationStatusEnum.WAITING_FIRST_APPROVAL.getDescription().equals(statusName) && reservationRegionId.equals(adminRegionId);
		} else if (AdminRoleEnum.ROLE_SECOND_APPROVER.getId().equals(roleId)) {
			// 2차 승인자 -> 1차 승인 대기, 2차 승인 대기 모두 체크 가능
			return ReservationStatusEnum.WAITING_FIRST_APPROVAL.getDescription().equals(statusName) || ReservationStatusEnum.WAITING_SECOND_APPROVAL.getDescription().equals(statusName);
		}
		return false;
	}
}

