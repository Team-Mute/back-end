package Team_Mute.back_end.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 회원가입 요청 DTO
 * 일반 사용자가 공간 예약 서비스에 가입할 때 사용
 * 이메일, 비밀번호, 이름, 소속 기업, 이메일 수신 정보 포함
 * UserController의 signUp API에서 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Schema(description = "회원가입 req DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDto {

	/**
	 * 사용자 이름
	 * - 필수 입력 항목
	 * - 2자 이상 50자 이하
	 * - 한글 또는 영문만 입력 가능
	 * - 정규식으로 특수문자 입력 차단
	 * - 실명 권장
	 */
	@Schema(description = "사용자 이름", example = "김뮤트")
	@NotBlank(message = "사용자 이름은 필수 입력 항목입니다.")
	@Size(min = 2, max = 50, message = "사용자 이름은 2자 이상 50자 이하로 입력해주세요.")
	@Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "사용자 이름은 한글, 영문만 입력 가능합니다.")
	private String userName;

	/**
	 * 사용자 이메일 주소
	 * - 필수 입력 항목
	 * - 이메일 형식 검증 적용
	 * - 최대 100자 이하
	 * - 로그인 ID로 사용
	 * - 중복 불가 (이메일 중복 체크 API로 사전 확인)
	 */
	@Schema(description = "사용자 이메일", example = "mute@example.com")
	@NotBlank(message = "이메일은 필수 입력 항목입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@Size(max = 100, message = "이메일은 100자 이하로 입력해주세요.")
	private String userEmail;

	/**
	 * 사용자 비밀번호
	 * - 필수 입력 항목
	 * - 8자 이상 20자 이하
	 * - 영문, 숫자, 특수문자(@$!%*?&) 각각 최소 1개씩 포함 필수
	 * - 정규식으로 비밀번호 강도 검증
	 * - BCrypt 알고리즘으로 암호화하여 저장
	 */
	@Schema(description = "사용자 비밀번호", example = "example123!")
	@NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
	@Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
	@Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[a-zA-Z\\d@$!%*?&]+$",
		message = "비밀번호는 영문, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다.")
	private String userPwd;

	/**
	 * 소속 기업명
	 * - 필수 입력 항목
	 * - 1자 이상 50자 이하
	 * - CorpInfoController의 기업 검색 API를 통해 조회한 기업명 사용 권장
	 * - 법인에 등록된 정식 기업명 입력
	 */
	@Schema(description = "소속 기업명", example = "(주)뮤트")
	@NotBlank(message = "회사명은 필수 입력 항목입니다.")
	@Size(min = 1, max = 50, message = "회사명은 1자 이상 50자 이하여야 합니다.")
	private String companyName;

	/**
	 * 이메일 수신 동의 여부
	 * - 필수 선택 항목 (true 또는 false)
	 * - true: 예약 관련 이메일 알림 수신 동의
	 * - false: 이메일 알림 수신 거부
	 * - 예약 승인/거부 알림에 사용
	 */
	@Schema(description = "이메일 수신 동의", example = "true")
	@NotNull(message = "이메일 알림 동의 여부를 선택해주세요.")
	private Boolean agreeEmail;
}
