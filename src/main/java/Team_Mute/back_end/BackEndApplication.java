package Team_Mute.back_end;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(servers = {
	// 로컬 서버 URL
	@Server(url = "http://localhost:8080", description = "Local Server URL"),
	
	// 배포된 서버 URL
	@Server(url = "https://healthy-velvet-sinhan-space-rental-36c4aa0c.koyeb.app", description = "Production Server URL")
})
@SpringBootApplication
public class BackEndApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackEndApplication.class, args);
	}

}
