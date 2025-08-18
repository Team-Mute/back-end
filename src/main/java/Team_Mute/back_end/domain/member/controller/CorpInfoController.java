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

@Tag(name = "기업 검색 API", description = "기업 검색 관련 API 명세")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CorpInfoController {

	private final CorpInfoService corpInfoService;

	@Operation(summary = "기업 검색", description = "기업명을 받아 법인에 등록된 기업명 리스트를 반환합니다.")
	@PostMapping("/corpname")
	public ResponseEntity<CorpNameSearchResponseDto> searchCorpName(
		@Valid @RequestBody CorpNameSearchRequestDto request) throws URISyntaxException {
		CorpNameSearchResponseDto response = corpInfoService.searchCorporateNames(request);
		return ResponseEntity.ok(response);
	}
}
