package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 관리자 회원가입 요청 DTO
 * 마스터 관리자가 새로운 관리자 계정을 생성할 때 사용
 * 이메일, 이름, 전화번호, 역할, 담당 지역 정보 포함
 * AdminController의 adminSignUp API에서 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
public class AdminSignupRequestDto {
	/**
	 * 관리자 역할 ID
	 * - 필수 입력 항목
	 * - 0: 마스터 관리자 (최고 권한)
	 * - 1: 2차 승인자 (최종 승인)
	 * - 2: 1차 승인자 (지역별 예약 1차 검토)
	 * - @Min과 @Max로 0~2 범위 제한
	 */
	@NotNull(message = "역할 ID는 필수 입력 값입니다.")
	@Min(value = 0, message = "역할 ID는 0 또는 1 또는 2여야 합니다.")
	@Max(value = 2, message = "역할 ID는 0 또는 1 또는 2여야 합니다.")
	private Integer roleId;

	/**
	 * 담당 지역명
	 * - 선택 항목 (null 허용)
	 * - 1차 승인자의 담당 지역 설정
	 * - 마스터 관리자, 2차 승인자는 담당 지역 불필요
	 */
	private String regionName;

	/**
	 * 관리자 이메일 주소
	 * - 필수 입력 항목
	 * - 이메일 형식 검증 적용
	 * - 로그인 ID로 사용
	 * - 중복 불가 (서비스 레이어에서 검증)
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
