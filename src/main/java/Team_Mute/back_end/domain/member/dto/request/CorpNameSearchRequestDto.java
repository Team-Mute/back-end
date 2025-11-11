package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 기업명 검색 요청 DTO
 * 외부 기업 정보 조회 API와 연동하여 법인에 등록된 기업 검색
 * 사용자 회원가입 시 소속 기업 검색 기능에 사용
 * CorpInfoController의 searchCorpName API에서 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
public class CorpNameSearchRequestDto {
	/**
	 * 검색할 기업명 키워드
	 * - 필수 입력 항목
	 * - 최소 1자 이상 입력
	 * - 외부 API에 전달하여 검색 수행
	 */
	@NotBlank(message = "기업명은 필수 입력 항목입니다.")
	@Size(min = 1, message = "기업명은 한 글자 이상 입력해야 합니다.")
	private String corpNm;

	/**
	 * 페이지 번호
	 * - 선택 항목 (기본값은 서비스 레이어에서 설정)
	 * - 1부터 시작하는 페이지 인덱스
	 * - 검색 결과가 많을 경우 페이징 처리
	 * - @Min으로 1 이상 제한
	 */
	@Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
	private Integer pageNo;
}
