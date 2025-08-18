package Team_Mute.back_end.domain.smsAuth.exception;

import Team_Mute.back_end.global.CustomException;

public class InvalidVerificationException extends CustomException {
	public InvalidVerificationException(String message) {
		super(400, message);
	}
}
