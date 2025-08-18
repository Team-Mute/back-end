package Team_Mute.back_end.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminInfoResponse {
	private Integer roleId;
	private String regionName;
	private String userEmail;
	private String userName;
	private String userPhone;
}
