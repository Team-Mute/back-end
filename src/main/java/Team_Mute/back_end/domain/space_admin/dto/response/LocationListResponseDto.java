package Team_Mute.back_end.domain.space_admin.dto.response;

/**
 * 위치 목록 응답 DTO
 * - 공간 등록/수정 시 선택 가능한 위치 리스트를 내려줄 때 사용
 * - 클라이언트에서 드롭다운/검색용 데이터로 활용
 */
public record LocationListResponseDto(
	Integer locationId, // 위치 고유 ID (tb_locations.location_id)
	String locationName, // 위치명 (예: "신한 스퀘어 브릿지 서울")
	String addressRoad, // 도로명 주소 및 추가 설명
	String postalCode // 우편번호
) {
}
