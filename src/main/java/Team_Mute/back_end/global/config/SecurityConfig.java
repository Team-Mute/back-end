package Team_Mute.back_end.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import Team_Mute.back_end.domain.member.jwt.JwtService;
import Team_Mute.back_end.domain.member.session.SessionStore;
import Team_Mute.back_end.domain.member.util.JwtAuthFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, SessionStore store) throws
		Exception {
		http
			.cors(
				Customizer.withDefaults()
			)
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authz -> authz
				.requestMatchers("/api/auth/login", "/api/sms/**", "/api/users/signup",
					"/api/users/check-email", "/api/corpname", "/api/users/reset-password", "/api/admin/auth/login",
					"/api/admin/reset-password", "/v3/api-docs/**",
					"/swagger-ui/**",
					"/swagger-resources/**",
					"/swagger-ui.html", "/api/auth/refresh", "/api/admin/auth/refresh",
					"/api/reservations/available-dates", "/api/reservations/available-times", "/api/spaces/**",
					"/api/spaces-user/**")
				.permitAll()
				.requestMatchers("/api/admin/signup")
				.hasAnyRole("0")
				.requestMatchers("/api/admin/**", "/api/admin/account/**", "/api/spaces-admin/**",
					"/api/reservations-admin/**")
				.hasAnyRole("0", "1", "2")
				.requestMatchers("/api/previsit/**")
				.hasRole("3")
				.requestMatchers("/api/users/account/**", "/api/auth/logout")
				.authenticated()
				.anyRequest()
				.authenticated()
			)
			.addFilterBefore(new JwtAuthFilter(jwtService, store), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
