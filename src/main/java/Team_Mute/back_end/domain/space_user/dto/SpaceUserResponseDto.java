package Team_Mute.back_end.domain.space_user.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * 공간 검색 결과 리스트 항목을 반환하는 인터페이스 기반 DTO
 * 간결한 공간 정보를 제공하며, DB Native Query의 결과를 직접 매핑
 */
@JsonPropertyOrder({
	"spaceId", "spaceName", "spaceDescription",
	"spaceCapacity", "categoryId", "categoryName", "tagNames", "location", "spaceImageUrl"
})
public interface SpaceUserResponseDto {

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
	 * 공간에 대한 설명을 반환
	 *
	 * @return 공간 설명
	 */
	String getSpaceDescription();

	/**
	 * 공간의 최대 수용 인원을 반환
	 *
	 * @return 최대 수용 인원
	 */
	Integer getSpaceCapacity();

	/**
	 * 공간의 카테고리 ID를 반환
	 *
	 * @return 카테고리 ID (예: 1=미팅룸, 2=행사장)
	 */
	Integer getCategoryId();

	/**
	 * 공간의 카테고리 이름(예: "미팅룸", "행사장")을 반환
	 *
	 * @return 카테고리 이름
	 */
	String getCategoryName();

	/**
	 * 공간에 연결된 편의시설 태그 이름 배열을 반환
	 *
	 * @return 태그 이름 배열
	 */
	String[] getTagNames();

	/**
	 * 공간의 위치 및 주소 정보를 JSON 문자열로 반환
	 * 예시: {@code {"locationName":"신한 스퀘어브릿지 서울", "addressRoad":"서울특별시 명동10길 52 (충무로2가 65-4)", "accessInfo":"명동역 도보 2분"}}
	 *
	 * @return 위치 및 주소 정보 (JSON Raw Value)
	 */
	@JsonRawValue
	String getLocation();

	/**
	 * 공간의 메인 이미지 URL을 반환
	 *
	 * @return 메인 이미지 URL
	 */
	String getSpaceImageUrl();
}
