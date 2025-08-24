package Team_Mute.back_end.domain.member.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import Team_Mute.back_end.domain.member.service.AdminService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

	private final AdminService userService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		userService.createInitialAdmin();
	}
}
