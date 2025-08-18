package Team_Mute.back_end.domain.member.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import Team_Mute.back_end.domain.member.dto.external.corp_api.CorpApiBodyDto;
import Team_Mute.back_end.domain.member.dto.external.corp_api.CorpApiResponseDto;
import Team_Mute.back_end.domain.member.dto.external.corp_api.CorpApiWrapperDto;
import Team_Mute.back_end.domain.member.dto.request.CorpNameSearchRequestDto;
import Team_Mute.back_end.domain.member.dto.response.CorpNameSearchResponseDto;
import Team_Mute.back_end.domain.member.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorpInfoService {

	private final RestTemplate restTemplate;

	@Value("${corp.api.base-url}")
	private String baseUrl;

	@Value("${corp.api.serviceKey}")
	private String serviceKey;

	private static final int NUM_OF_ROWS = 20;
	private static final int MAX_RESULTS = 20;

	public CorpNameSearchResponseDto searchCorporateNames(CorpNameSearchRequestDto request) {
		String corpNm = request.getCorpNm();
		int startPageNo = (request.getPageNo() != null && request.getPageNo() > 0) ? request.getPageNo() : 1;

		Set<String> uniqueCorpNames = new HashSet<>();
		int pageNo = startPageNo;
		int totalCount = Integer.MAX_VALUE;

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		HttpEntity<String> entity = new HttpEntity<>(headers);

		while (uniqueCorpNames.size() < MAX_RESULTS && (pageNo - 1) * NUM_OF_ROWS < totalCount) {
			try {
				String finalUrlAsString = String.format(
					"%s?ServiceKey=%s&pageNo=%d&numOfRows=%d&resultType=json&corpNm=%s",
					baseUrl,
					URLEncoder.encode(serviceKey, StandardCharsets.UTF_8),
					pageNo,
					NUM_OF_ROWS,
					URLEncoder.encode(corpNm, StandardCharsets.UTF_8)
				);

				URI finalUri = URI.create(finalUrlAsString);

				log.info("외부 API 호출 (최종 URI): {}", finalUri);

				ResponseEntity<CorpApiWrapperDto> responseEntity = restTemplate.exchange(
					finalUri,
					HttpMethod.GET,
					entity,
					CorpApiWrapperDto.class
				);

				if (responseEntity.getBody() == null || responseEntity.getBody().getResponse() == null) {
					log.info("API 응답 본문이 비어있거나 response 객체가 없습니다.");
					break;
				}

				CorpApiResponseDto response = responseEntity.getBody().getResponse();

				if (response.getHeader() == null) {
					throw new ExternalApiException("기업 정보 조회 실패: API 응답 없음");
				}

				CorpApiBodyDto body = response.getBody();
				if (body == null || body.getItems() == null || body.getItems().getItem() == null) {
					break;
				}

				if (pageNo == startPageNo) {
					totalCount = body.getTotalCount();
					if (totalCount < MAX_RESULTS) {
						log.info("총 결과가 {}개 이므로, {}개만 수집합니다.", totalCount, totalCount);
					}
				}

				body.getItems().getItem().forEach(item -> {
					if (item != null && item.getCorpNm() != null && !item.getCorpNm()
						.isBlank()) {
						uniqueCorpNames.add(item.getCorpNm().trim());
					}
				});

				if (totalCount < MAX_RESULTS) {
					break;
				}

				pageNo++;

			} catch (Exception e) {
				log.error("외부 API 호출 중 오류 발생", e);
				if (!(e instanceof ExternalApiException)) {
					throw new ExternalApiException("외부 API 서버와 통신 중 오류가 발생했습니다: " + e.getMessage());
				}
				throw e;
			}
		}

		int lastSearchedPage = pageNo > startPageNo ? pageNo - 1 : startPageNo;

		log.info("총 {}개의 고유 기업명 검색 완료. 마지막 검색 페이지: {}", uniqueCorpNames.size(), lastSearchedPage);
		return new CorpNameSearchResponseDto(new ArrayList<>(uniqueCorpNames), lastSearchedPage);
	}
}
