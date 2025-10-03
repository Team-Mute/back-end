package Team_Mute.back_end.domain.space_admin.dto.response;

/**
 * 태그 리스트 조회 응답 DTO
 * - 공간 등록/수정 시 태그 선택을 위한 리스트에 사용
 */
public record TagListResponseDto(
	Integer tagId, // 태그 고유 ID (PK)
	String tagName // 태그 이름 (예: WIFI, 화이트보드, TV 등)
) {
}
