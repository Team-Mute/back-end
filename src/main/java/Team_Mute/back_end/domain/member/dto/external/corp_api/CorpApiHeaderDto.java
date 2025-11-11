package Team_Mute.back_end.domain.member.dto.external.corp_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 외부 기업 정보 조회 API 응답의 Header 부분을 매핑하는 DTO
 * API 호출의 성공/실패 여부와 결과 메시지를 포함
 * XML 또는 JSON 형식의 API 응답을 자바 객체로 자동 변환
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // API 응답에 정의되지 않은 필드가 있어도 무시하여 역직렬화 에러 방지
public class CorpApiHeaderDto {
	/**
	 * API 호출 결과 코드
	 */
	private String resultCode;

	/**
	 * API 호출 결과 메시지
	 */
	private String resultMsg;
}
