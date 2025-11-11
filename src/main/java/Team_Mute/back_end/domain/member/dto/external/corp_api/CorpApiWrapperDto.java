package Team_Mute.back_end.domain.member.dto.external.corp_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 외부 기업 정보 조회 API의 최상위 래퍼 DTO
 * 일부 API는 <response> 태그를 한 번 더 감싸는 구조를 가지는 경우가 있어 이를 처리하기 위한 래퍼 클래스
 * API 응답에 정의되지 않은 필드가 있어도 무시하여 역직렬화 에러 방지
 * XML 구조가 중첩된 경우 사용하며, response 필드를 통해 실제 응답 데이터에 접근
 * API 응답 구조에 따라 CorpApiResponseDto 대신 이 클래스를 사용할 수 있음
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CorpApiWrapperDto {
	/**
	 * API 응답 객체
	 * - 실제 응답 데이터를 담고 있는 CorpApiResponseDto 인스턴스
	 * - 중첩된 XML 구조를 처리하기 위한 래퍼 필드
	 */
	private CorpApiResponseDto response;
}
