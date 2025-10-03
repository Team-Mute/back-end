package Team_Mute.back_end.domain.space_admin.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 리스트 조회 응답 DTO
 *
 * @param adminId           관리자 ID
 * @param adminNameWithRole 포맷된 관리자 이름 (예: 홍길동(1차 승인자))
 */
public record AdminListResponseDto(
	@Schema(description = "관리자 ID")
	Long adminId,

	@Schema(description = "포맷된 관리자 이름 (예: 홍길동(1차 승인자))")
	String adminNameWithRole
) {
}
