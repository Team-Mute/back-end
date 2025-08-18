package Team_Mute.back_end.domain.member.exception;

import Team_Mute.back_end.global.CustomException;

public class DuplicateEmailException extends CustomException {
	public DuplicateEmailException(String message) {
		super(400, message);
	}
}

