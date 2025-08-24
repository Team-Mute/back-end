package Team_Mute.back_end.domain.member.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SignupResponseDto {
	private String message;
	private Long userId;
	private Integer roleId; // roleId 필드 추가

	// 생성자 수정
	public SignupResponseDto(String message, Long userId, Integer roleId) {
		this.message = message;
		this.userId = userId;
		this.roleId = roleId;
	}
}
