package Team_Mute.back_end.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger (OpenAPI 3.0) 설정 클래스
 * API 문서 자동 생성 및 JWT 인증 테스트 환경 제공
 *
 * 접근 URL:
 * - Swagger UI: /swagger-ui.html
 * - API Docs: /v3/api-docs
 */
@Configuration
public class SwaggerConfig {

	/**
	 * OpenAPI 설정
	 *
	 * @return OpenAPI 객체
	 */
	@Bean
	public OpenAPI openAPI() {
		String jwtSchemeName = "jwtAuth";

		// JWT 인증 요구사항 정의
		SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

		// JWT 보안 스키마 정의
		Components components = new Components()
			.addSecuritySchemes(jwtSchemeName, new SecurityScheme()
				.name(jwtSchemeName)
				.type(SecurityScheme.Type.HTTP)  // HTTP 인증
				.scheme("bearer")                 // Bearer 토큰
				.bearerFormat("JWT"));            // JWT 형식

		return new OpenAPI()
			// API 기본 정보
			.info(new Info()
				.title("신한금융희망재단 공간 대여 API")
				.description("API 명세서")
				.version("1.0.0"))
			// 모든 API에 JWT 인증 적용
			.addSecurityItem(securityRequirement)
			.components(components);
	}
}
