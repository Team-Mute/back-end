package Team_Mute.back_end.domain.reservation.exception;

import Team_Mute.back_end.global.CustomException;

public class InvalidInputValueException extends CustomException {
	public InvalidInputValueException(String message) {
		super(400, message);
	}
}
