package Team_Mute.back_end.domain.space_admin.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 공간 리스트 조회 응답 DTO (Projection Interface)
 * - 공간 목록 화면/검색 결과 조회에 사용
 * - 상세 정보 대신 리스트에 필요한 핵심 필드만 포함
 * - Jackson 직렬화 시 지정된 순서대로 JSON 출력
 */
@JsonPropertyOrder({
	"spaceId", "spaceName", "regionName", "regionId", "adminName",
	"spaceImageUrl", "spaceIsAvailable"
})
public interface SpaceListResponseDto {
	Integer getSpaceId(); // 공간 고유 ID

	String getSpaceName(); // 공간 이름

	String getRegionName(); // 지역 이름 (예: 서울, 인천 등)

	Integer getRegionId(); // 지역 고유 ID

	String getAdminName(); // 담당 관리자 이름

	String getSpaceImageUrl(); // 대표 이미지 경로

	Boolean getSpaceIsAvailable(); // 공간 활성화 여부 (true=운영중, false=비활성)
}
