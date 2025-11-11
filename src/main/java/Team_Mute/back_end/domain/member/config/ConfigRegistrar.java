package Team_Mute.back_end.domain.member.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import Team_Mute.back_end.domain.member.jwt.JwtConfig;

/**
 * 설정 클래스를 스프링 빈으로 등록하는 설정 등록자 클래스
 * JWT 관련 설정 클래스를 포함하여 외부 설정 파일(application.properties)의 값을 자바 객체로 매핑
 * EnableConfigurationProperties를 통해 ConfigurationProperties 어노테이션이 적용된 클래스를 활성화하고 빈으로 등록
 * 향후 다른 설정 클래스(Redis, S3, SMS 등)도 이곳에서 통합 관리 가능
 *
 * @author Team Mute
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties({JwtConfig.class})
public class ConfigRegistrar {
}
