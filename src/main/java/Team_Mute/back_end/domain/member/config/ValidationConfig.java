package Team_Mute.back_end.domain.member.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * 유효성 검증 관련 설정을 담당하는 클래스
 * @Validated 어노테이션을 통해 애플리케이션 전역에서 메서드 레벨 유효성 검증 활성화
 * JSR-303 Bean Validation 표준을 지원하며, 컨트롤러 메서드의 파라미터 검증에 사용
 * 향후 커스텀 Validator, 메시지 소스 설정, ValidationConfigurationCustomizer 등 추가 설정 확장 가능
 *
 * @author Team Mute
 * @since 1.0
 */
@Configuration
@Validated
public class ValidationConfig {
	/**
	 * 추가적인 validation 설정이 필요한 경우 여기에 작성
	 *
	 * 현재 프로젝트에서는:
	 * - DashboardAdminController의 @Min, @Max 어노테이션 검증
	 * - RequestDto 클래스의 @NotNull, @NotEmpty, @Size 등 필드 검증
	 * - 메서드 파라미터의 제약 조건 자동 검증
	 */
}
