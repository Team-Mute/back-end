package Team_Mute.back_end.domain.reservation.exception;

import Team_Mute.back_end.global.CustomException;

public class ForbiddenAccessException extends CustomException {
	public ForbiddenAccessException(String message) {
		super(403, message);
	}
}
