package Team_Mute.back_end.domain.dashboard_admin.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException() {
		super("해당 사용자를 찾을 수 없습니다.");
	}

	public UserNotFoundException(String message) {
		super(message);
	}
}
