package Team_Mute.back_end.domain.member.dto.external.corp_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 외부 기업 정보 조회 API 응답의 Body 부분을 매핑하는 DTO
 * API 응답의 실제 데이터 부분을 담당하며, 검색 결과 항목 리스트와 페이징 정보를 포함
 * XML 또는 JSON 형식의 API 응답을 자바 객체로 자동 변환
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // API 응답에 정의되지 않은 필드가 있어도 무시하여 역직렬화 에러 방지
public class CorpApiBodyDto {
	/**
	 * 기업 정보 검색 결과 항목 리스트
	 * - item 배열을 포함하는 items 객체
	 */
	private CorpApiItemsDto items;

	/**
	 * 한 페이지당 조회할 데이터 개수
	 * - API 요청 시 설정한 페이지당 행 수
	 */
	private int numOfRows;

	/**
	 * 현재 페이지 번호
	 * - 1부터 시작하는 페이지 인덱스
	 */
	private int pageNo;

	/**
	 * 전체 검색 결과 개수
	 * - 검색 조건에 해당하는 총 기업 수
	 */
	private int totalCount;
}
