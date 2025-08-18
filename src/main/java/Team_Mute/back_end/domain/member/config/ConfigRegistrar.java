package Team_Mute.back_end.domain.member.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import Team_Mute.back_end.domain.member.jwt.JwtConfig;

@Configuration
@EnableConfigurationProperties({JwtConfig.class})
public class ConfigRegistrar {
}
