package Team_Mute.back_end.domain.member.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 기업명 검색 응답 DTO
 * 외부 기업 정보 조회 API에서 받은 데이터를 가공하여 클라이언트에 반환
 * CorpInfoController의 searchCorpName API 응답으로 사용
 * 사용자 회원가입 시 소속 기업 입력란의 자동완성 기능에 활용
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorpNameSearchResponseDto {
	/**
	 * 검색된 기업명 목록
	 * - 외부 API에서 조회한 기업명 리스트
	 * - 법인에 등록된 정식 기업명
	 * - 검색 키워드와 부분 일치하는 결과
	 * - 회원가입 시 소속 기업 선택에 사용
	 */
	private List<String> item;

	/**
	 * 마지막으로 검색한 페이지 번호
	 * - 현재 조회한 페이지 번호 (1부터 시작)
	 * - 추가 검색 시 다음 페이지 요청에 사용
	 * - 페이징 처리를 위한 정보
	 */
	private int pageNo;
}
