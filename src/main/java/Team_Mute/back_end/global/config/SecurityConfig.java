package Team_Mute.back_end.global.config;

import Team_Mute.back_end.domain.member.jwt.JwtService;
import Team_Mute.back_end.domain.member.session.SessionStore;
import Team_Mute.back_end.domain.member.util.JwtAuthFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 클래스
 * JWT 기반 인증/인가 및 역할 기반 접근 제어 설정
 * <p>
 * 주요 기능:
 * - JWT 인증 필터 등록
 * - 역할별 엔드포인트 접근 제어
 * - CORS 설정
 * - CSRF 비활성화 (JWT 사용)
 * - Stateless 세션 관리
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/**
	 * Security Filter Chain 설정
	 *
	 * @param http       HttpSecurity 객체
	 * @param jwtService JWT 토큰 서비스
	 * @param store      Redis 세션 저장소
	 * @return SecurityFilterChain
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, SessionStore store) throws
		Exception {
		http
			// CORS 설정 활성화 (WebConfig의 CorsFilter 사용)
			.cors(Customizer.withDefaults())

			// CSRF 비활성화 (JWT 사용으로 불필요)
			.csrf(AbstractHttpConfigurer::disable)

			// Stateless 세션 관리 (서버에 세션 저장 안 함)
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// URL 패턴별 접근 권한 설정
			.authorizeHttpRequests(authz -> authz
				// 인증 없이 접근 가능한 엔드포인트
				.requestMatchers("/api/auth/login", "/api/sms/**", "/api/users/signup",
					"/api/users/check-email", "/api/corpName", "/api/users/reset-password", "/api/admin/auth/login",
					"/api/admin/reset-password", "/v3/api-docs/**",
					"/swagger-ui/**",
					"/swagger-resources/**",
					"/swagger-ui.html", "/api/auth/refresh", "/api/admin/auth/refresh",
					"/api/reservations/fully-available-dates", "/api/reservations/available-dates", "/api/reservations/available-times",
					"/api/spaces/regions", "/api/spaces/tags",
					"/api/spaces-user/**", "/api/invitations/**")
				.permitAll()

				// 마스터 관리자(ROLE_0)만 접근 가능
				.requestMatchers("/api/admin/signup")
				.hasAnyRole("0")

				// 모든 관리자(ROLE_0, 1, 2) 접근 가능
				.requestMatchers("/api/admin/**", "/api/admin/account/**", "/api/spaces-admin/**",
					"/api/reservations-admin/**", "/api/dashboard-admin/**", "/api/spaces/categories", "/api/spaces/locations/{regionId}")
				.hasAnyRole("0", "1", "2")

				// 일반 사용자(ROLE_3)만 접근 가능
				.requestMatchers("/api/previsit/**")
				.hasRole("3")

				// 인증된 사용자만 접근 가능
				.requestMatchers("/api/users/account/**", "/api/auth/logout")
				.authenticated()

				// 그 외 모든 요청은 인증 필요
				.anyRequest()
				.authenticated()
			)

			// JwtAuthFilter를 UsernamePasswordAuthenticationFilter 이전에 추가
			// JWT 토큰을 검증하여 SecurityContext에 인증 정보 설정
			.addFilterBefore(new JwtAuthFilter(jwtService, store), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
