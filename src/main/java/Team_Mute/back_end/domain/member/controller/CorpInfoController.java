package Team_Mute.back_end.domain.member.controller;

import java.net.URISyntaxException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Team_Mute.back_end.domain.member.dto.request.CorpNameSearchRequestDto;
import Team_Mute.back_end.domain.member.dto.response.CorpNameSearchResponseDto;
import Team_Mute.back_end.domain.member.service.CorpInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 기업 정보 검색 관련 요청을 처리하는 컨트롤러입니다.
 * 외부 기업 정보 조회 API(CUFinder)와 연동하여 법인에 등록된 기업명 검색 기능을 제공
 * 사용자 회원가입 시 소속 기업 검색 기능에 사용
 * RestTemplate을 통해 외부 API와 통신하며, 검색 결과를 가공하여 클라이언트에 반환
 * 인증 없이 접근 가능한 공개 API
 *
 * @author Team Mute
 * @since 1.0
 */
@Tag(name = "기업 검색 API", description = "기업 검색 관련 API 명세")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CorpInfoController {

	private final CorpInfoService corpInfoService;

	/**
	 * 기업명 검색
	 * - 사용자가 입력한 키워드로 법인에 등록된 기업명 리스트를 조회
	 * - 외부 기업 정보 조회 API(CUFinder API)를 호출하여 검색 수행
	 * - 검색 결과를 가공하여 기업명 정보 반환
	 * - 회원가입 시 소속 기업 검색 기능에 활용
	 * - 인증 없이 접근 가능하며, 누구나 기업명 검색 가능
	 * - RestTemplate을 사용하여 외부 API와 HTTP 통신 수행
	 *
	 * @param request 기업명 검색 요청 DTO (검색 키워드 포함)
	 * @return 검색된 기업 정보 리스트를 포함하는 {@code ResponseEntity<CorpNameSearchResponseDto>}
	 * @throws URISyntaxException 외부 API URI 생성 시 문법 오류가 발생한 경우
	 */
	@Operation(summary = "기업 검색", description = "기업명을 받아 법인에 등록된 기업명 리스트를 반환합니다.")
	@PostMapping("/corpName")
	public ResponseEntity<CorpNameSearchResponseDto> searchCorpName(
		@Valid @RequestBody CorpNameSearchRequestDto request
	) throws URISyntaxException {
		// 외부 API 호출을 통한 기업명 검색 수행
		CorpNameSearchResponseDto response = corpInfoService.searchCorporateNames(request);
		return ResponseEntity.ok(response);
	}
}
