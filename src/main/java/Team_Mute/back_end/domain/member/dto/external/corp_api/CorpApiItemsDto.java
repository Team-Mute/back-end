package Team_Mute.back_end.domain.member.dto.external.corp_api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Data;

/**
 * 외부 기업 정보 조회 API의 기업 정보 항목 리스트를 매핑하는 DTO
 * 여러 개의 기업 정보(CorpApiItemDto)를 리스트로 담는 컨테이너 역할
 * XML 형식의 item 배열을 자바 List로 변환하기 위한 Jackson XML 어노테이션 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // API 응답에 정의되지 않은 필드가 있어도 무시하여 역직렬화 에러 방지
public class CorpApiItemsDto {
	/**
	 * 기업 정보 항목 리스트
	 * - XML의 반복되는 <item> 태그를 List<CorpApiItemDto>로 매핑
	 * - @JacksonXmlElementWrapper(useWrapping = false): <items> 태그 내부에 바로 <item>이 위치 (래퍼 태그 없음)
	 * - @JacksonXmlProperty(localName = "item"): XML 태그명을 "item"으로 지정
	 */
	@JacksonXmlElementWrapper(useWrapping = false) // <items> 내부에 바로 <item> 태그가 반복되는 구조
	@JacksonXmlProperty(localName = "item") // XML 요소명을 "item"으로 매핑
	private List<CorpApiItemDto> item;
}
