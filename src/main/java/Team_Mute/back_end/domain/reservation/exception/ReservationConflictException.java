package Team_Mute.back_end.domain.reservation.exception;

public class ReservationConflictException extends RuntimeException {
	public ReservationConflictException(String message) {
		super(message);
	}
}
