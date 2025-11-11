package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 사용자 비밀번호 초기화 요청 DTO
 * 비밀번호를 잊어버린 사용자의 비밀번호를 초기화할 때 사용
 * 임시 비밀번호를 생성하여 이메일로 발송
 * UserController의 resetPassword API에서 사용
 * 인증 없이 접근 가능한 공개 API
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
public class PasswordResetRequestDto {
	/**
	 * 비밀번호를 초기화할 사용자의 이메일 주소
	 * - 필수 입력 항목
	 * - 이메일 형식 검증 적용
	 * - 해당 이메일로 임시 비밀번호 발송
	 * - 로그인 후 비밀번호 변경 권장
	 * - Token Version 증가로 기존 JWT 토큰 무효화
	 */
	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String userEmail;
}
