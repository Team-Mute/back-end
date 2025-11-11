package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 사용자 정보 수정 요청 DTO
 * 사용자가 본인의 계정 정보를 수정할 때 사용
 * 이름, 이메일, 이메일 수신 동의 정보 수정 가능
 * UserController의 updateUserInfo API에서 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
public class UserInfoUpdateRequestDto {
	/**
	 * 사용자 이메일 주소
	 * - 필수 입력 항목
	 * - 이메일 형식 검증 적용
	 * - 계정 식별을 위해 사용
	 * - 이메일 변경은 보안상 제한될 수 있음 (서비스 정책에 따라)
	 */
	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String userEmail;

	/**
	 * 사용자 이름
	 * - 필수 입력 항목
	 * - 실명 권장
	 * - 예약 정보에 표시될 이름
	 */
	@NotBlank(message = "이름은 필수 입력 값입니다.")
	private String userName;

	/**
	 * 이메일 수신 동의 여부
	 * - 필수 선택 항목 (true 또는 false)
	 * - true: 예약 관련 이메일 알림 수신 동의
	 * - false: 이메일 알림 수신 거부
	 * - 마이페이지에서 언제든지 변경 가능
	 */
	@NotNull(message = "이메일 수신 동의 여부를 입력해주세요.")
	private Boolean agreeEmail;
}
