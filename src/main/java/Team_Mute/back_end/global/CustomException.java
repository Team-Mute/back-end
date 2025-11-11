package Team_Mute.back_end.global;

/**
 * 커스텀 예외 클래스
 * HTTP 상태 코드와 메시지를 함께 전달하는 범용 예외
 *
 * 특징:
 * - RuntimeException 상속 (Unchecked Exception)
 * - HTTP 상태 코드 포함
 * - GlobalExceptionHandler에서 처리
 */
public class CustomException extends RuntimeException {
	private final int statusCode;

	/**
	 * CustomException 생성자
	 *
	 * @param statusCode HTTP 상태 코드
	 * @param message 예외 메시지
	 */
	public CustomException(int statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
	}

	/**
	 * HTTP 상태 코드 조회
	 * @return 상태 코드
	 */
	public int getStatusCode() {
		return statusCode;
	}
}
