package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 사용자 로그인 요청 DTO
 * - Java 17의 Record 타입을 사용하여 불변 데이터 클래스 정의
 * - 이메일과 비밀번호는 필수, 디바이스 및 IP 정보는 선택
 *
 * @param email 사용자 이메일 (필수)
 * @param password 사용자 비밀번호 (필수)
 * @param device 로그인 디바이스 정보 (선택, 예: "Android", "iOS", "Web")
 * @param ip 클라이언트 IP 주소 (선택)
 *
 * @author Team Mute
 * @since 1.0
 */
public record LoginRequest(
	@NotBlank(message = "이메일은 필수 입력 항목입니다.")
	String email,

	@NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
	String password,

	String device,

	String ip
) {
}
