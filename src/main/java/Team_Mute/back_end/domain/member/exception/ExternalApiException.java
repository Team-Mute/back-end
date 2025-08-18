package Team_Mute.back_end.domain.member.exception;

import Team_Mute.back_end.global.CustomException;

public class ExternalApiException extends CustomException {
	public ExternalApiException(String message) {
		super(502, message);
	}
}

