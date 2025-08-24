package Team_Mute.back_end.domain.reservation.exception;

import Team_Mute.back_end.global.CustomException;

public class ResourceNotFoundException extends CustomException {
	public ResourceNotFoundException(String message) {
		super(404, message);
	}
}
