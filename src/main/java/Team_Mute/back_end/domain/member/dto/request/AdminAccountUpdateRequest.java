package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 관리자 정보 수정 요청 DTO
 * 관리자가 본인의 계정 정보를 수정할 때 사용
 * 이름, 전화번호, 이메일, 담당 지역 정보 수정 가능
 * AdminController의 updateAdminInfo API에서 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
public class AdminAccountUpdateRequest {
	/**
	 * 담당 지역명
	 * - 선택 항목 (null 허용)
	 * - 1차 또는 2차 승인자의 담당 지역 수정
	 * - 마스터 관리자는 담당 지역이 없을 수 있음
	 */
	private String regionName;

	/**
	 * 관리자 이메일 주소
	 * - 필수 입력 항목
	 * - 이메일 형식 검증 적용
	 * - 계정 식별을 위해 사용
	 */
	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String userEmail;

	/**
	 * 관리자 이름
	 * - 필수 입력 항목
	 * - 실명으로 입력
	 */
	@NotBlank(message = "이름은 필수 입력 값입니다.")
	private String userName;

	/**
	 * 관리자 전화번호
	 * - 필수 입력 항목
	 * - 연락 가능한 전화번호
	 */
	@NotBlank(message = "전화번호는 필수 입력 값입니다.")
	private String userPhone;
}
