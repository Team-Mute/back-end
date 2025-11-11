package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 관리자 비밀번호 수정 요청 DTO
 * 관리자가 본인의 비밀번호를 변경할 때 사용
 * 기존 비밀번호 검증 후 새 비밀번호로 변경
 * AdminController의 updateAdminPassword API에서 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
public class AdminPasswordUpdateRequest {
	/**
	 * 현재 비밀번호
	 * - 필수 입력 항목
	 * - 본인 확인을 위해 기존 비밀번호 검증
	 * - BCrypt 해시 비교를 통한 일치 여부 확인
	 */
	@NotBlank(message = "현재 비밀번호는 필수 입력 값입니다.")
	private String password;

	/**
	 * 새 비밀번호
	 * - 필수 입력 항목
	 * - 최소 8자 이상
	 * - BCrypt 알고리즘으로 암호화하여 저장
	 * - 비밀번호 변경 시 Token Version 증가로 기존 JWT 토큰 무효화
	 */
	@NotBlank(message = "새 비밀번호는 필수 입력 값입니다.")
	@Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
	private String newPassword;
}
