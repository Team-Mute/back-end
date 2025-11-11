package Team_Mute.back_end.global.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 에러 응답 DTO
 * API 요청 처리 중 발생한 예외를 클라이언트에 전달하기 위한 공통 에러 응답 형식
 * GlobalExceptionHandler에서 예외 발생 시 이 DTO로 에러 정보를 래핑하여 반환
 * 클라이언트가 에러를 인지하고 적절한 처리를 할 수 있도록 상세 정보 제공
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
	/**
	 * 에러 메시지
	 * - 사용자에게 표시할 에러 내용
	 * - 한글로 작성된 상세 메시지
	 * - 예: "이메일 형식이 올바르지 않습니다.", "권한이 없습니다."
	 */
	private String message;

	/**
	 * HTTP 상태 코드
	 * - 400: Bad Request (잘못된 요청)
	 * - 401: Unauthorized (인증 실패)
	 * - 403: Forbidden (권한 없음)
	 * - 404: Not Found (리소스 없음)
	 * - 500: Internal Server Error (서버 오류)
	 */
	private int status;

	/**
	 * 에러 발생 시각
	 * - 서버에서 예외가 발생한 정확한 시간
	 * - ISO 8601 형식으로 직렬화
	 * - 로깅 및 디버깅 용도
	 */
	private LocalDateTime timestamp;

	/**
	 * ErrorResponseDto 생성자 (timestamp 자동 설정)
	 * - timestamp를 현재 시각으로 자동 설정
	 * - GlobalExceptionHandler에서 주로 사용
	 *
	 * @param message 에러 메시지
	 * @param status HTTP 상태 코드
	 */
	public ErrorResponseDto(String message, int status) {
		this.message = message;
		this.status = status;
		this.timestamp = LocalDateTime.now();
	}
}
