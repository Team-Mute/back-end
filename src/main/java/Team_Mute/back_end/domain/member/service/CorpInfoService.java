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

/**
 * 기업 정보 조회 서비스 클래스
 * 외부 기업 정보 조회 API(예: 공공데이터포털, 국세청 API 등)와 연동하여 법인 등록 기업 정보를 조회
 * 회원가입 시 소속 기업 검색 기능에 사용
 * RestTemplate을 사용한 HTTP 통신으로 외부 API 호출
 *
 * 주요 기능:
 * - 기업명 키워드 검색
 * - 페이징 처리를 통한 대량 데이터 조회
 * - 중복 기업명 자동 제거 (HashSet 활용)
 * - 최대 20개의 고유 기업명 반환
 * - XML/JSON 형식의 API 응답 자동 파싱
 *
 * 보안 고려사항:
 * - ServiceKey는 application.properties에서 환경 변수로 관리
 * - URL 파라미터는 UTF-8로 인코딩하여 인젝션 공격 방지
 *
 * @author Team Mute
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CorpInfoService {

	/**
	 * RestTemplate 인스턴스
	 * - Spring에서 제공하는 HTTP 클라이언트
	 * - RestTemplateConfig에서 Bean으로 등록된 인스턴스 주입
	 * - 타임아웃, 메시지 컨버터 등 설정 적용됨
	 * - XML 응답을 자동으로 DTO로 변환 (MappingJackson2XmlHttpMessageConverter)
	 */
	private final RestTemplate restTemplate;

	/**
	 * 외부 기업 정보 조회 API의 기본 URL
	 */
	@Value("${corp.api.base-url}")
	private String baseUrl;

	/**
	 * 외부 기업 정보 조회 API의 인증 키
	 */
	@Value("${corp.api.serviceKey}")
	private String serviceKey;

	/**
	 * API 호출 시 한 페이지당 조회할 데이터 개수
	 * - 외부 API의 numOfRows 파라미터에 사용
	 * - 20개씩 페이징 처리
	 * - 너무 큰 값은 API 성능 저하 및 타임아웃 발생 가능
	 */
	private static final int NUM_OF_ROWS = 20;

	/**
	 * 클라이언트에 반환할 최대 결과 개수
	 * - 중복 제거 후 최대 20개의 고유 기업명만 반환
	 * - 프론트엔드 자동완성 UI의 성능을 고려한 제한
	 */
	private static final int MAX_RESULTS = 20;

	/**
	 * 기업명 검색
	 * - 키워드로 기업명 검색 후 중복 제거된 기업명 목록 반환
	 * - 페이징 처리를 통해 충분한 결과 수집 (최대 20개)
	 * - HashSet을 사용하여 중복 자동 제거
	 * - 외부 API의 페이지를 순회하면서 고유 기업명 수집
	 *
	 * 처리 흐름:
	 * 1. 요청 DTO에서 검색 키워드와 시작 페이지 추출
	 * 2. HTTP 헤더 설정 (Accept: application/json)
	 * 3. 페이징 루프 시작 (최대 20개 수집까지 또는 전체 결과 소진까지)
	 * 4. 각 페이지마다 API 호출 (URL 인코딩 적용)
	 * 5. 응답 파싱 및 검증 (null 체크, 헤더 검증)
	 * 6. 기업명 추출 및 중복 제거 (HashSet에 추가)
	 * 7. 조기 종료 조건: 20개 수집 완료 또는 API 결과 소진
	 * 8. 수집된 고유 기업명과 마지막 페이지 번호 반환
	 *
	 * 성능 최적화:
	 * - HashSet으로 O(1) 중복 체크
	 * - 조기 종료로 불필요한 API 호출 방지
	 * - 페이징으로 메모리 효율적 처리
	 *
	 * 예외 처리:
	 * - API 호출 실패 시 ExternalApiException 발생
	 * - 네트워크 오류, 타임아웃, 잘못된 응답 등 처리
	 *
	 * @param request 기업명 검색 요청 DTO (검색 키워드, 시작 페이지 번호)
	 * @return CorpNameSearchResponseDto (고유 기업명 목록, 마지막 검색 페이지)
	 * @throws ExternalApiException 외부 API 호출 실패 또는 응답 오류 시
	 */
	public CorpNameSearchResponseDto searchCorporateNames(CorpNameSearchRequestDto request) {
		// 1. 요청 DTO에서 검색 키워드 추출
		String corpNm = request.getCorpNm();

		// 2. 시작 페이지 번호 설정 (null이거나 0 이하면 1로 설정)
		int startPageNo = (request.getPageNo() != null && request.getPageNo() > 0)
			? request.getPageNo()
			: 1;

		// 3. 중복 제거를 위한 HashSet 초기화
		// - HashSet은 자동으로 중복을 제거하고 O(1) 시간 복잡도로 존재 여부 확인
		Set<String> uniqueCorpNames = new HashSet<>();

		// 4. 현재 페이지 번호 초기화
		int pageNo = startPageNo;

		// 5. 전체 결과 개수 초기화 (첫 API 호출 후 업데이트)
		int totalCount = Integer.MAX_VALUE;

		// 6. HTTP 헤더 설정
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));  // JSON 응답 요청
		HttpEntity<String> entity = new HttpEntity<>(headers);

		// 7. 페이징 루프 시작
		// 조건: (1) 수집된 고유 기업명이 20개 미만이고, (2) 아직 전체 결과를 다 조회하지 않았을 때
		while (uniqueCorpNames.size() < MAX_RESULTS && (pageNo - 1) * NUM_OF_ROWS < totalCount) {
			try {
				// 8. API 요청 URL 생성
				// - ServiceKey, pageNo, numOfRows, resultType, corpNm 파라미터 포함
				// - UTF-8로 URL 인코딩하여 특수문자 처리 및 인젝션 공격 방지
				String finalUrlAsString = String.format(
					"%s?ServiceKey=%s&pageNo=%d&numOfRows=%d&resultType=json&corpNm=%s",
					baseUrl,
					URLEncoder.encode(serviceKey, StandardCharsets.UTF_8),
					pageNo,
					NUM_OF_ROWS,
					URLEncoder.encode(corpNm, StandardCharsets.UTF_8)
				);

				// 9. String을 URI 객체로 변환
				URI finalUri = URI.create(finalUrlAsString);

				log.info("외부 API 호출 (최종 URI): {}", finalUri);

				// 10. RestTemplate을 사용하여 외부 API 호출
				// - HTTP GET 메서드
				// - CorpApiWrapperDto 타입으로 자동 파싱 (XML to Object)
				// - RestTemplateConfig에서 설정한 MappingJackson2XmlHttpMessageConverter 사용
				ResponseEntity<CorpApiWrapperDto> responseEntity = restTemplate.exchange(
					finalUri,
					HttpMethod.GET,
					entity,
					CorpApiWrapperDto.class
				);

				// 11. 응답 본문이 비어있는지 확인
				if (responseEntity.getBody() == null || responseEntity.getBody().getResponse() == null) {
					log.info("API 응답 본문이 비어있거나 response 객체가 없습니다.");
					break;  // 루프 종료
				}

				// 12. 응답에서 response 객체 추출
				CorpApiResponseDto response = responseEntity.getBody().getResponse();

				// 13. 응답 헤더 검증 (API 호출 성공 여부 확인)
				if (response.getHeader() == null) {
					throw new ExternalApiException("기업 정보 조회 실패: API 응답 없음");
				}

				// 14. 응답 바디 추출 및 검증
				CorpApiBodyDto body = response.getBody();
				if (body == null || body.getItems() == null || body.getItems().getItem() == null) {
					break;  // 더 이상 결과가 없으면 루프 종료
				}

				// 15. 첫 페이지 조회 시 전체 결과 개수 업데이트
				if (pageNo == startPageNo) {
					totalCount = body.getTotalCount();
					if (totalCount < MAX_RESULTS) {
						log.info("총 결과가 {}개 이므로, {}개만 수집합니다.", totalCount, totalCount);
					}
				}

				// 16. 기업명 추출 및 HashSet에 추가 (중복 자동 제거)
				body.getItems().getItem().forEach(item -> {
					// null 체크 및 공백 체크
					if (item != null && item.getCorpNm() != null && !item.getCorpNm().isBlank()) {
						// trim()으로 앞뒤 공백 제거 후 추가
						uniqueCorpNames.add(item.getCorpNm().trim());
					}
				});

				// 17. 전체 결과가 20개 미만인 경우 조기 종료
				if (totalCount < MAX_RESULTS) {
					break;
				}

				// 18. 다음 페이지로 이동
				pageNo++;

			} catch (Exception e) {
				// 19. 예외 처리
				log.error("외부 API 호출 중 오류 발생", e);

				// 이미 ExternalApiException인 경우 그대로 재발생
				if (!(e instanceof ExternalApiException)) {
					// 다른 예외는 ExternalApiException으로 래핑
					throw new ExternalApiException("외부 API 서버와 통신 중 오류가 발생했습니다: " + e.getMessage());
				}
				throw e;
			}
		}

		// 20. 마지막으로 검색한 페이지 번호 계산
		// - 루프가 최소 한 번 실행되었으면 pageNo - 1, 아니면 startPageNo
		int lastSearchedPage = pageNo > startPageNo ? pageNo - 1 : startPageNo;

		log.info("총 {}개의 고유 기업명 검색 완료. 마지막 검색 페이지: {}", uniqueCorpNames.size(), lastSearchedPage);

		// 21. 응답 DTO 생성 및 반환
		// - HashSet을 ArrayList로 변환 (순서는 보장되지 않음)
		// - 마지막 검색 페이지 번호 포함
		return new CorpNameSearchResponseDto(new ArrayList<>(uniqueCorpNames), lastSearchedPage);
	}
}
