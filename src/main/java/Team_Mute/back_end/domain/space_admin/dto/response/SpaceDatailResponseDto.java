package Team_Mute.back_end.domain.space_admin.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * 공간 상세 조회 응답 DTO (Projection Interface)
 * - 공간 등록 정보 및 연관 데이터(지역, 카테고리, 위치, 태그, 운영시간, 휴무일 등)를 포함
 * - Repository의 Native Query / JPQL 결과를 인터페이스 기반 Projection으로 매핑
 * - Jackson 직렬화 시 지정한 순서대로 필드가 JSON에 출력됨
 */
@JsonPropertyOrder({
	"spaceId", "spaceName", "region", "category", "location", "adminName",
	"spaceCapacity", "spaceLocation", "spaceDescription", "spaceImageUrl", "detailImageUrls",
	"tagNames", "spaceIsAvailable", "reservationWay", "spaceRules", "operations", "closedDays",
	"regDate", "updDate"
})
public interface SpaceDatailResponseDto {
	Integer getSpaceId(); // 공간 고유 ID

	String getAdminName(); // 담당 관리자 이름

	String getSpaceName(); // 공간 이름

	Integer getSpaceCapacity(); // 수용 인원

	String getSpaceDescription(); // 공간 설명

	String getSpaceImageUrl(); // 대표 이미지 경로

	String[] getDetailImageUrls(); // 상세 이미지 경로 배열

	Boolean getSpaceIsAvailable(); // 활성화 여부

	String[] getTagNames(); // 태그 이름 배열

	String getReservationWay(); // 예약 방식 설명

	String getSpaceRules(); // 이용 수칙

	@JsonRawValue
	String getOperations(); // 운영 시간 JSON (예: [{"day":1,"from":"09:00","to":"18:00","isOpen":true}, ...])

	@JsonRawValue
	String getClosedDays(); // 휴무일 JSON (예: [{"from":"2025-09-01T00:00:00","to":"2025-09-02T23:59:59"}, ...])

	java.time.LocalDateTime getRegDate(); // 등록일

	java.time.LocalDateTime getUpdDate(); // 수정일

	@JsonRawValue
	String getRegion();   // JSON text: {"regionId":1,"regionName":"명동"}

	@JsonRawValue
	String getCategory(); // JSON text: {"categoryId":1,"categoryName":"미팅룸"}

	@JsonRawValue
	String getLocation(); // JSON text: {"locationId":1,"addressRoad":"..."}
}
