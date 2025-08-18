package Team_Mute.back_end.domain.member.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import Team_Mute.back_end.domain.member.jwt.JwtService;
import Team_Mute.back_end.domain.member.session.SessionStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwt;
	private final SessionStore store;

	public JwtAuthFilter(JwtService jwt, SessionStore store) {
		this.jwt = jwt;
		this.store = store;
	}

	@SuppressWarnings("checkstyle:NeedBraces")
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
		throws ServletException, IOException {

		String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
		if (auth == null || !auth.startsWith("Bearer ")) {
			log.debug("Authorization 헤더가 없거나 'Bearer '로 시작하지 않습니다. URI: {}", req.getRequestURI());
			chain.doFilter(req, res);
			return;
		}

		String token = auth.substring(7);
		log.info("인증 토큰 발견: {}", token);

		try {
			Jws<Claims> jws = jwt.parse(token);
			Claims c = jws.getPayload();
			log.info("토큰 파싱 성공. Subject(userId): {}, Audience: {}", c.getSubject(), c.getAudience());

			String jti = c.getId();
			if (jti != null && store.isBlacklisted(jti)) {
				log.warn("블랙리스트에 등록된 토큰입니다. JTI: {}", jti);
				res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String sid = (String)c.get("sid");
			if (sid == null || store.getSessionJson(sid) == null) {
				log.warn("유효하지 않은 세션 ID입니다. SID: {}", sid);
				res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			if (c.getAudience().contains("user-service")) {
				processUserToken(c);
			} else if (c.getAudience().contains("admin-service")) {
				processAdminToken(c);
			} else {
				log.warn("알 수 없는 Audience 입니다: {}", c.getAudience());
				res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			log.info("사용자 인증 성공. SecurityContext에 저장 완료.");

		} catch (Exception e) {
			log.error("토큰 검증 중 예외 발생: {}", e.getMessage(), e);
			res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		chain.doFilter(req, res);
	}

	private void processUserToken(Claims claims) {
		String userId = claims.getSubject();
		Integer roleId = claims.get("roles", Integer.class);

		Collection<GrantedAuthority> auths = new ArrayList<>();
		if (roleId != null) {
			auths.add(new SimpleGrantedAuthority("ROLE_" + roleId));
		}

		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userId, null, auths);
		authentication.setDetails(Map.of(
			"cid", claims.get("cid"),
			"sid", claims.get("sid"),
			"ver", claims.get("ver")
		));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void processAdminToken(Claims claims) {
		String userId = claims.getSubject();
		Integer roleId = claims.get("roleId", Integer.class);

		Collection<GrantedAuthority> auths = new ArrayList<>();
		if (roleId != null) {
			auths.add(new SimpleGrantedAuthority("ROLE_" + roleId));
		}

		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userId, null, auths);

		Map<String, Object> details = new HashMap<>();
		details.put("sid", claims.get("sid"));
		details.put("ver", claims.get("ver"));

		Integer regionId = claims.get("regionId", Integer.class);
		if (regionId != null) {
			details.put("regionId", regionId);
		}

		authentication.setDetails(details);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
