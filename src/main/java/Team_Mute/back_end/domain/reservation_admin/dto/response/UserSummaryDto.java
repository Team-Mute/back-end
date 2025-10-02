package Team_Mute.back_end.domain.reservation_admin.dto.response;

public record UserSummaryDto(
	Long id,
	String name,
	String email,
	String company // ADAPT: UserCompany가 있으면 회사명 매핑
) {
}
