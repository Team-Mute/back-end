package Team_Mute.back_end.domain.space_user.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * 특정 공간의 상세 정보를 조회하여 반환하는 인터페이스 기반 DTO
 * - DB Native Query의 결과를 직접 매핑하며, JSON 형태로 반환되는 필드에는 {@code @JsonRawValue}가 적용
 */
@JsonPropertyOrder({
	"spaceId", "spaceName", "regionName", "categoryName", "spaceCapacity", "spaceDescription",
	"spaceImageUrl", "reservationWay", "spaceRules", "spaceIsAvailable", "manager",
	"location", "detailImageUrls", "tagNames", "operations", "closedDays",
})
public interface SpaceUserDtailResponseDto {
	/**
	 * 공간의 고유 ID를 반환
	 *
	 * @return 공간 ID
	 */
	Integer getSpaceId();

	/**
	 * 공간 이름을 반환
	 *
	 * @return 공간 이름
	 */
	String getSpaceName();

	/**
	 * 공간이 속한 지역 이름을 반환
	 *
	 * @return 지역 이름
	 */
	String getRegionName();

	/**
	 * 공간의 카테고리 이름(예: "미팅룸", "행사")을 반환
	 *
	 * @return 카테고리 이름
	 */
	String getCategoryName();

	/**
	 * 공간의 최대 수용 인원을 반환
	 *
	 * @return 최대 수용 인원
	 */
	Integer getSpaceCapacity();

	/**
	 * 공간에 대한 상세 설명을 반환
	 *
	 * @return 공간 설명
	 */
	String getSpaceDescription();

	/**
	 * 공간의 메인 이미지 URL을 반환
	 *
	 * @return 메인 이미지 URL
	 */
	String getSpaceImageUrl();

	/**
	 * 공간의 예약 방법을 반환
	 *
	 * @return 예약 방법
	 */
	String getReservationWay();

	/**
	 * 공간 이용 수칙을 반환
	 *
	 * @return 공간 이용 수칙
	 */
	String getSpaceRules();

	/**
	 * 공간 활성화 여부 (예약 가능 여부 판단)
	 *
	 * @return 공간 활성화 여부 (예약 가능 여부 판단)
	 */
	boolean getSpaceIsAvailable();

	/**
	 * 공간 담당자 정보를 JSON 문자열로 반환
	 * 예시: {@code {"managerName":"홍길동", "managerPhone":"010-1234-5678"}}
	 *
	 * @return 담당자 정보 (JSON Raw Value)
	 */
	@JsonRawValue
	String getManager();

	/**
	 * 공간의 위치 및 주소 정보를 JSON 문자열로 반환
	 * 예시: {@code {"locationName":"신한 스퀘어브릿지 서울", "addressRoad":"서울특별시 명동10길 52 (충무로2가 65-4)", "accessInfo":"명동역 도보 2분"}}
	 *
	 * @return 위치 및 주소 정보 (JSON Raw Value)
	 */
	@JsonRawValue
	String getLocation();

	/**
	 * 공간의 상세 이미지 URL 배열을 반환
	 *
	 * @return 상세 이미지 URL 배열
	 */
	String[] getDetailImageUrls();

	/**
	 * 공간에 연결된 편의시설 태그 이름 배열을 반환
	 * 예시: {@code {"day": 1, "from": "09:00", "to": "18:00", "isOpen": true}},
	 * day는 월(1), 화(2), ..., 일(7)
	 *
	 * @return 태그 이름 배열
	 */
	String[] getTagNames();

	/**
	 * 공간의 운영 시간을 요일별로 나타내는 JSON 배열 문자열을 반환
	 * 예시: {@code { "from": "2025-10-04T00:00:00", "to": "2025-10-03T23:59:59"}}
	 *
	 * @return 운영 시간 정보 (JSON Raw Value)
	 */
	@JsonRawValue
	String getOperations();


	/**
	 * 공간의 휴무일을 나타내는 JSON 배열 문자열을 반환
	 *
	 * @return 휴무일 정보 (JSON Raw Value)
	 */
	@JsonRawValue
	String getClosedDays();
}
