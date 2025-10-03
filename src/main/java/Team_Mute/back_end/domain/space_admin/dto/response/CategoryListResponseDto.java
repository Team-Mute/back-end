package Team_Mute.back_end.domain.space_admin.dto.response;

/**
 * 카테고리 목록 응답 DTO
 * - 공간 카테고리 목록을 조회할 때 사용되는 응답 데이터 형식
 * - 예: "미팅룸", "행사장" 등과 같은 카테고리 목록을 내려줄 때 활용
 */
public record CategoryListResponseDto(
	Integer categoryId, // 카테고리 ID (PK: tb_space_categories.category_id)
	String categoryName // 카테고리 이름 (예: 미팅룸, 강연장, 행사장)
) {
}
