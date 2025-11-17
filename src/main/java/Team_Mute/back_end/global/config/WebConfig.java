package Team_Mute.back_end.global.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Web 설정 클래스
 * CORS(Cross-Origin Resource Sharing) 정책 설정
 *
 * 목적:
 * - 프론트엔드(React)와 백엔드(Spring Boot) 간 도메인이 다른 경우 통신 허용
 * - 개발 환경에서 localhost:3000 허용
 */
@Configuration
public class WebConfig {

	/**
	 * CORS 필터 Bean 등록
	 *
	 * @return CorsFilter
	 */
	@Bean
	public CorsFilter corsFilter() {
		CorsConfiguration config = new CorsConfiguration();

		// 허용할 Origin 패턴 (프론트엔드 URL)
		config.setAllowedOriginPatterns(List.of(
			"https://localhost:3000",  // HTTPS 개발 환경
			"http://localhost:3000",    // HTTP 개발 환경
			"https://shinhan-reservation-app.vercel.app",
			"http://shinhan-reservation-app.vercel.app",
			"https://healthy-velvet-sinhan-space-rental-36c4aa0c.koyeb.app/",
			"http://healthy-velvet-sinhan-space-rental-36c4aa0c.koyeb.app/"
		));

		// 허용할 HTTP 메서드
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

		// 허용할 헤더 (모든 헤더 허용)
		config.setAllowedHeaders(List.of("*"));

		// 자격 증명(쿠키, Authorization 헤더 등) 허용
		config.setAllowCredentials(true);

		// Preflight 요청 캐시 시간 (1시간)
		config.setMaxAge(3600L);

		// 모든 경로에 CORS 설정 적용
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		return new CorsFilter(source);
	}
}
