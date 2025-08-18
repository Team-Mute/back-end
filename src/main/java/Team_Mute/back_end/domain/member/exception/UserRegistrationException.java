package Team_Mute.back_end.domain.member.exception;

import Team_Mute.back_end.global.CustomException;

public class UserRegistrationException extends CustomException {
	public UserRegistrationException(String message) {
		super(500, message);
	}
}
