package Team_Mute.back_end.domain.member.dto.response;

import java.time.LocalDateTime;

import Team_Mute.back_end.domain.member.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoResponseDto {
	private Long userId;
	private Integer roleId;
	private Integer companyId;
	private String userEmail;
	private String userName;
	private String userPhone;
	private LocalDateTime regDate;
	private LocalDateTime updDate;
	private Boolean agreeEmail;
	private Boolean agreeSms;
	private Boolean agreeLocation;
	private String companyName;
	private String roleName;

	public static UserInfoResponseDto fromEntity(User user) {
		return UserInfoResponseDto.builder()
			.userId(user.getUserId())
			.roleId(user.getRoleId())
			.companyId(user.getCompanyId())
			.userEmail(user.getUserEmail())
			.userName(user.getUserName())
			.userPhone(user.getUserPhone())
			.regDate(user.getRegDate())
			.updDate(user.getUpdDate())
			.agreeEmail(user.getAgreeEmail())
			.agreeSms(user.getAgreeSms())
			.agreeLocation(user.getAgreeLocation())
			.companyName(user.getUserCompany().getCompanyName())
			.roleName(user.getUserRole().getRoleName())
			.build();
	}
}
