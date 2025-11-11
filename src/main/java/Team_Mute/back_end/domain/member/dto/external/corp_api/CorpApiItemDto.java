package Team_Mute.back_end.domain.member.dto.external.corp_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 외부 기업 정보 조회 API의 개별 기업 정보 항목을 매핑하는 DTO
 * 하나의 기업에 대한 상세 정보(기업명, 사업자등록번호, 설립일 등)를 담는 클래스
 * XML 또는 JSON 형식의 API 응답을 자바 객체로 자동 변환
 * 회원가입 시 소속 기업 검색 결과로 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // API 응답에 정의되지 않은 필드가 있어도 무시하여 역직렬화 에러 방지
public class CorpApiItemDto {
	/**
	 * 법인명 또는 기업명
	 * - 국세청에 등록된 정식 기업명
	 */
	private String corpNm;

	/**
	 * 사업자등록번호
	 * - 10자리 숫자 형식의 사업자 식별 번호 (하이픈 제거된 형태)
	 */
	private String bzno;

	/**
	 * 기업 설립일
	 * - "YYYYMMDD" 형식의 설립 날짜 (예: "20200101")
	 */
	private String enpEstbDt;
}
