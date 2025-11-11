package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 관리자 비밀번호 초기화 요청 DTO
 * 비밀번호를 잊어버린 관리자의 비밀번호를 초기화할 때 사용
 * 임시 비밀번호를 생성하여 이메일로 발송
 * AdminController의 resetAdminPassword API에서 사용
 * 인증 없이 접근 가능한 공개 API
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
public class AdminPasswordResetRequest {
	/**
	 * 비밀번호를 초기화할 관리자의 이메일 주소
	 * - 필수 입력 항목
	 * - 이메일 형식 검증 적용
	 * - 해당 이메일로 임시 비밀번호 발송
	 * - 로그인 후 비밀번호 변경 권장
	 */
	@NotBlank(message = "초기화할 회원의 이메일은 필수입니다.")
	@Email
	private String userEmail;
}
