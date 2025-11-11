package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 관리자 계정 삭제 요청 DTO
 * 마스터 관리자가 다른 관리자 계정을 삭제할 때 사용
 * 삭제할 관리자의 이메일 주소를 입력받아 계정 식별
 * AdminController의 deleteAdminAccount API에서 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
public class AdminAccountDeleteRequest {
	/**
	 * 삭제할 관리자의 이메일 주소
	 * - 필수 입력 항목
	 * - 이메일 형식 검증 적용
	 * - 마스터 관리자는 자기 자신을 삭제할 수 없도록 서비스 레이어에서 추가 검증
	 */
	@NotBlank(message = "삭제할 회원의 이메일은 필수입니다.")
	@Email
	private String userEmail;
}
