package Team_Mute.back_end.domain.member.util;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import Team_Mute.back_end.domain.member.dto.external.corp_api.CorpApiItemDto;

/**
 * 외부 API 응답의 커스텀 역직렬화기
 * Jackson 라이브러리의 JsonDeserializer를 확장하여 유연한 JSON 파싱 제공
 * 단일 객체와 배열 모두를 List로 통일하여 처리
 *
 * 문제 상황:
 * - 외부 기업 정보 API는 결과가 1개일 때 단일 객체 반환
 * - 결과가 2개 이상일 때 배열 반환
 * - 일관성 없는 응답 구조로 파싱 에러 발생
 *
 * 해결 방법:
 * - JsonToken을 확인하여 배열인지 객체인지 판단
 * - 배열이면 List로 직접 파싱
 * - 단일 객체면 List로 래핑하여 반환
 * - 항상 List<CorpApiItemDto> 타입으로 통일
 *
 * 사용처:
 * - CorpApiItemsDto 클래스의 item 필드
 * - @JsonDeserialize(using = CustomItemDeserializer.class) 어노테이션으로 적용
 *
 * @author Team Mute
 * @since 1.0
 */
public class CustomItemDeserializer extends JsonDeserializer<List<CorpApiItemDto>> {

	/**
	 * JSON을 List<CorpApiItemDto>로 역직렬화
	 * - 현재 토큰이 배열 시작인지 객체 시작인지 확인
	 * - 배열이면 전체를 List로 파싱
	 * - 단일 객체면 하나의 요소를 가진 List로 변환
	 *
	 * 처리 흐름:
	 * 1. JsonParser에서 현재 토큰 확인
	 * 2. START_ARRAY 토큰인 경우:
	 *    - ObjectMapper로 List<CorpApiItemDto> 타입으로 파싱
	 *    - 배열의 모든 요소를 CorpApiItemDto로 변환
	 * 3. 단일 객체인 경우:
	 *    - ObjectMapper로 CorpApiItemDto 하나만 파싱
	 *    - Collections.singletonList()로 1개 요소를 가진 불변 List 생성
	 *
	 * JSON 예시:
	 * - 단일 객체: { "corpNm": "신한금융그룹", ... }
	 *   → [{ "corpNm": "신한금융그룹", ... }]
	 *
	 * - 배열: [{ "corpNm": "신한금융그룹", ... }, { "corpNm": "신한은행", ... }]
	 *   → [{ "corpNm": "신한금융그룹", ... }, { "corpNm": "신한은행", ... }]
	 *
	 * @param p JsonParser 인스턴스 (Jackson의 JSON 파싱 도구)
	 * @param ctxt DeserializationContext (역직렬화 컨텍스트)
	 * @return List<CorpApiItemDto> (단일 객체도 List로 래핑)
	 * @throws IOException JSON 파싱 실패 시
	 */
	@Override
	public List<CorpApiItemDto> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		// 1. JsonParser에서 ObjectMapper 추출
		ObjectMapper mapper = (ObjectMapper)p.getCodec();

		// 2. 현재 토큰이 배열 시작인지 확인
		if (p.getCurrentToken() == JsonToken.START_ARRAY) {
			// 3. 배열인 경우: List<CorpApiItemDto>로 직접 파싱
			return mapper.readValue(p,
				mapper.getTypeFactory().constructCollectionType(List.class, CorpApiItemDto.class));
		}

		// 4. 단일 객체인 경우: CorpApiItemDto 하나만 파싱
		CorpApiItemDto singleItem = mapper.readValue(p, CorpApiItemDto.class);

		// 5. 단일 객체를 1개 요소를 가진 불변 List로 래핑
		return Collections.singletonList(singleItem);
	}
}
