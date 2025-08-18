package Team_Mute.back_end.domain.member.exception;

import Team_Mute.back_end.global.CustomException;

public class CompanyNotFoundException extends CustomException {
	public CompanyNotFoundException(String message) {
		super(400, message);
	}
}
