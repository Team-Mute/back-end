package Team_Mute.back_end.domain.reservation_admin.util;

import org.springframework.stereotype.Component;

@Component
public final class ReservationApprovalPolicy {

	private ReservationApprovalPolicy() {
	} // 인스턴스화 방지

	// ================== 상태 이름 상수 ==================
	public static final String STATUS_FIRST_PENDING = "1차 승인 대기";
	public static final String STATUS_SECOND_PENDING = "2차 승인 대기";
	public static final String STATUS_FINAL_APPROVED = "최종 승인 완료";

	// ================== 역할 상수 ==================
	public static final Long ROLE_SECOND_APPROVER = 1L; // 2차 승인자: 1,2차 승인 가능
	public static final Long ROLE_FIRST_APPROVER = 2L; // 1차 승인자: 1차만 가능

	// ================== 승인 가능 여부 (리스트 체크박스 및 승인 버튼 활성화 기준) ==================
	public static boolean isApprovableFor(Long roleId, String statusName) {
		if (ROLE_FIRST_APPROVER.equals(roleId)) {
			// 1차 승인자 → 1차 승인 대기만 체크 가능
			return STATUS_FIRST_PENDING.equals(statusName);
		} else if (ROLE_SECOND_APPROVER.equals(roleId)) {
			// 2차 승인자 → 1차 승인 대기, 2차 승인 대기 모두 체크 가능
			return STATUS_FIRST_PENDING.equals(statusName) || STATUS_SECOND_PENDING.equals(statusName);
		}
		return false;
	}

	// ================== 반려 가능 여부 (반려 버튼 활성화 기준) ==================
	public static boolean isRejectableFor(Long roleId, String statusName) {
		if (ROLE_FIRST_APPROVER.equals(roleId)) {
			// 1차 승인자 -> 1차 승인 대기일 때만 반려 가능
			return STATUS_FIRST_PENDING.equals(statusName);
		} else if (ROLE_FIRST_APPROVER.equals(roleId)) {
			// 2차 승인자 -> 1차 승인 대기, 2차 승인 대기 모두 체크 가능
			return STATUS_FIRST_PENDING.equals(statusName) || STATUS_SECOND_PENDING.equals(statusName);
		}
		return false;
	}
}

