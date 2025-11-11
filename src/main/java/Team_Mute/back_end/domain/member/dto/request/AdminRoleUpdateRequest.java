package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 관리자 권한 수정 요청 DTO
 * 마스터 관리자가 다른 관리자의 역할을 변경할 때 사용
 * 1차 승인자와 2차 승인자 간 역할 변경 가능
 * AdminController의 updateAdminRole API에서 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
public class AdminRoleUpdateRequest {
	/**
	 * 권한을 수정할 관리자의 이메일 주소
	 * - 필수 입력 항목
	 * - 이메일 형식 검증 적용
	 * - 대상 관리자 식별에 사용
	 * - 마스터 관리자는 자기 자신의 권한을 변경할 수 없음
	 */
	@NotBlank(message = "수정할 회원의 이메일은 필수입니다.")
	@Email
	private String userEmail;

	/**
	 * 새로운 역할 ID
	 * - 필수 입력 항목
	 * - 1: 2차 승인자 (최종 승인 담당)
	 * - 2: 1차 승인자 (지역별 예약 1차 검토 담당)
	 * - 0(마스터 관리자)으로는 변경 불가 (보안상 제한)
	 * - @Min과 @Max로 1~2 범위 제한
	 */
	@NotNull(message = "역할 ID는 필수 입력 값입니다.")
	@Min(value = 1, message = "역할 ID는 1 또는 2여야 합니다.")
	@Max(value = 2, message = "역할 ID는 1 또는 2여야 합니다.")
	private Integer roleId;
}
