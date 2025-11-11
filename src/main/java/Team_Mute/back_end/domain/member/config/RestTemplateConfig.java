package Team_Mute.back_end.domain.member.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 API 통신을 위한 RestTemplate 설정 클래스
 * HTTP 클라이언트인 RestTemplate 빈을 등록하고 메시지 컨버터를 설정하여 외부 RESTful API와의 통신 기능 제공
 * JSON 및 XML 형식의 데이터를 자바 객체로 변환하거나 반대로 변환하는 기능 포함
 * 주로 기업 정보 조회 API 외부 서비스 연동에 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Configuration
public class RestTemplateConfig {

	/**
	 * RestTemplate 빈 등록 및 메시지 컨버터 설정
	 * - Spring에서 제공하는 동기식 HTTP 클라이언트로 외부 REST API 호출에 사용
	 * - GET, POST, PUT, DELETE 등 다양한 HTTP 메서드 지원
	 * - MappingJackson2HttpMessageConverter: JSON 형식의 HTTP 요청/응답을 자바 객체로 자동 변환
	 * - MappingJackson2XmlHttpMessageConverter: XML 형식의 HTTP 요청/응답을 자바 객체로 자동 변환
	 * - 설정된 메시지 컨버터를 통해 API 응답 데이터를 DTO 객체로 직접 매핑 가능
	 * - CorpInfoService에서 외부 기업 정보 API 호출에 활용
	 *
	 * @return RestTemplate 인스턴스 (외부 API 호출 및 데이터 변환에 사용)
	 */
	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		messageConverters.add(new MappingJackson2HttpMessageConverter());
		messageConverters.add(new MappingJackson2XmlHttpMessageConverter());
		restTemplate.setMessageConverters(messageConverters);
		return restTemplate;
	}
}
