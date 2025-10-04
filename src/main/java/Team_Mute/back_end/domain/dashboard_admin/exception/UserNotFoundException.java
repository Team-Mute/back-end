package Team_Mute.back_end.domain.dashboard_admin.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 사용자를 찾을 수 없을 때 발생하는 커스텀 RuntimeException
 * 이 예외 발생 시 HTTP 404 Not Found 상태 코드를 응답
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException() {
		super("해당 사용자를 찾을 수 없습니다.");
	}

	public UserNotFoundException(String message) {
		super(message);
	}
}
