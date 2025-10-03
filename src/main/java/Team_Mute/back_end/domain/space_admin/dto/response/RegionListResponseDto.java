package Team_Mute.back_end.domain.space_admin.dto.response;

/**
 * 지역 리스트 조회 응답 DTO
 * - 공간 검색 또는 예약 목록 조회 시, 특정 지역으로 필터링할 때 사용
 */
public record RegionListResponseDto(
	Integer regionId, // 지역 고유 ID (PK)
	String regionName //지역 이름 (예: 서울, 인천 등)
) {
}
