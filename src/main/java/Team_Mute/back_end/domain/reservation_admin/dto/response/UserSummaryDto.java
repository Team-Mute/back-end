package Team_Mute.back_end.domain.reservation_admin.dto.response;

/**
 * [예약 관리 -> 사용자 요약 정보] 응답 DTO (Java Record)
 * * 예약에 관련된 사용자의 핵심 정보를 간략하게 전달
 */
public record UserSummaryDto(
	/**
	 * 사용자 고유 ID
	 */
	Long id,

	/**
	 * 사용자 이름
	 */
	String name,

	/**
	 * 사용자 이메일
	 */
	String email,

	/**
	 * 사용자 소속 회사명
	 */
	String company // ADAPT: UserCompany가 있으면 회사명 매핑
) {
}
