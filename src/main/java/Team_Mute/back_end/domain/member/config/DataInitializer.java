package Team_Mute.back_end.domain.member.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import Team_Mute.back_end.domain.member.service.UserService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

	private final UserService userService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// 이 메서드는 애플리케이션이 완전히 시작된 후에 호출됩니다.
		userService.createInitialAdmin();
	}
}
