package Team_Mute.back_end.domain.member.dto.external.corp_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;

/**
 * 외부 기업 정보 조회 API의 전체 응답 구조를 매핑하는 DTO
 * API 응답의 최상위 구조로, header와 body를 포함
 * XML의 <response> 루트 요소를 자바 객체로 변환
 * RestTemplate의 exchange() 메서드에서 반환 타입으로 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // API 응답에 정의되지 않은 필드가 있어도 무시하여 역직렬화 에러 방지
@JacksonXmlRootElement(localName = "response") // XML 루트 요소명을 "response"로 지정
public class CorpApiResponseDto {
	/**
	 * API 응답 헤더
	 * - 결과 코드 및 메시지 포함
	 */
	private CorpApiHeaderDto header;

	/**
	 * API 응답 바디
	 * - 실제 기업 검색 결과 데이터 포함
	 * - 페이징 정보 및 검색 결과 항목 리스트
	 */
	private CorpApiBodyDto body;
}
