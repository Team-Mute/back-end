package Team_Mute.back_end.domain.member.dto.response;

import Team_Mute.back_end.domain.member.entity.Admin;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminSignupResponseDto {
	private Long adminId;
	private String adminEmail;
	private String adminName;
	private Integer roleId;

	// Admin 엔티티를 받아서 DTO로 변환하는 정적 팩토리 메서드
	public static AdminSignupResponseDto fromEntity(Admin admin) {
		return AdminSignupResponseDto.builder()
			.adminId(admin.getAdminId())
			.adminEmail(admin.getAdminEmail())
			.adminName(admin.getAdminName())
			.roleId(admin.getUserRole().getRoleId())
			.build();
	}
}
