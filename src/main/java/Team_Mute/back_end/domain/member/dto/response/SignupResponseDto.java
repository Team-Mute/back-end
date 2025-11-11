package Team_Mute.back_end.domain.member.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 회원가입 응답 DTO
 * 일반 사용자 회원가입 성공 시 클라이언트에 반환되는 데이터
 * UserController의 signUp API 응답으로 사용
 * 생성된 사용자의 기본 정보와 성공 메시지 포함
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@NoArgsConstructor
public class SignupResponseDto {
	/**
	 * 회원가입 성공 메시지
	 * - 사용자에게 표시할 환영 메시지
	 * - 예: "회원가입이 성공적으로 완료되었습니다."
	 */
	private String message;

	/**
	 * 생성된 사용자 ID
	 * - 데이터베이스에서 자동 생성된 Primary Key
	 * - 사용자 고유 식별자
	 * - 로그인 후 사용자 정보 조회에 사용
	 */
	private Long userId;

	/**
	 * 생성된 사용자 역할 ID
	 * - 일반 사용자는 기본적으로 3번 역할 부여 (일반 회원)
	 * - 향후 역할별 권한 분기 처리에 사용 가능
	 */
	private Integer roleId;

	/**
	 * SignupResponseDto 생성자
	 *
	 * @param message 회원가입 성공 메시지
	 * @param userId 생성된 사용자 ID
	 * @param roleId 생성된 사용자 역할 ID
	 */
	public SignupResponseDto(String message, Long userId, Integer roleId) {
		this.message = message;
		this.userId = userId;
		this.roleId = roleId;
	}
}
