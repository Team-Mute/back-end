package Team_Mute.back_end.domain.smsAuth.exception;

import Team_Mute.back_end.global.CustomException;

public class SmsSendingFailedException extends CustomException {
	public SmsSendingFailedException(String message) {
		super(500, message);
	}
}
