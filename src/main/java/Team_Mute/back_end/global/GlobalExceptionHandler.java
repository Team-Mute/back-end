package Team_Mute.back_end.global;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import Team_Mute.back_end.domain.member.exception.CompanyNotFoundException;
import Team_Mute.back_end.domain.member.exception.DuplicateEmailException;
import Team_Mute.back_end.domain.member.exception.ExternalApiException;
import Team_Mute.back_end.domain.member.exception.UserRegistrationException;
import Team_Mute.back_end.domain.reservation.exception.ForbiddenAccessException;
import Team_Mute.back_end.domain.reservation.exception.InvalidInputValueException;
import Team_Mute.back_end.domain.reservation.exception.PrevisitAlreadyExistsException;
import Team_Mute.back_end.domain.reservation.exception.ReservationConflictException;
import Team_Mute.back_end.domain.reservation.exception.ResourceNotFoundException;
import Team_Mute.back_end.global.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리 핸들러
 * 애플리케이션 전체의 예외를 일관된 형식으로 응답
 *
 * 주요 기능:
 * - 비즈니스 예외 처리 (중복 예약, 권한 없음 등)
 * - 검증 예외 처리 (DTO 유효성 검증 실패)
 * - HTTP 예외 처리 (잘못된 메서드, 파라미터 누락 등)
 * - 예기치 않은 예외 로깅 및 처리
 *
 * @Order(Ordered.HIGHEST_PRECEDENCE): 최우선 순위로 처리
 * @RestControllerAdvice: 모든 컨트롤러에 적용
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	/**
	 * 예약 중복 예외 처리 (409 Conflict)
	 * 같은 시간대에 이미 예약이 존재하는 경우
	 */
	@ExceptionHandler(ReservationConflictException.class)
	public ResponseEntity<ErrorResponseDto> handleReservationConflict(ReservationConflictException ex) {
		ErrorResponseDto errorResponse = new ErrorResponseDto(
			ex.getMessage(),
			HttpStatus.CONFLICT.value(),
			LocalDateTime.now()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
	}

	/**
	 * HTTP 메서드 미지원 예외 처리 (405 Method Not Allowed)
	 * POST 요청이 필요한데 GET 요청을 보낸 경우 등
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponseDto> handleHttpRequestMethodNotSupported(
		HttpRequestMethodNotSupportedException e) {
		log.error("Method Not Allowed: {} {} - Supported methods are {}", e.getMethod(), e.getLocalizedMessage(),
			e.getSupportedMethods());

		String supportedMethods = String.join(", ", Objects.requireNonNull(e.getSupportedMethods()));
		String message = String.format("해당 경로는 %s 메서드를 지원하지 않습니다. (지원하는 메서드: %s)", e.getMethod(), supportedMethods);

		ErrorResponseDto errorResponse = new ErrorResponseDto(message, HttpStatus.METHOD_NOT_ALLOWED.value());

		return ResponseEntity
			.status(HttpStatus.METHOD_NOT_ALLOWED)
			.header("Allow", supportedMethods)
			.body(errorResponse);
	}

	/**
	 * 필수 파라미터 누락 예외 처리 (400 Bad Request)
	 * @RequestParam(required=true)인 파라미터가 누락된 경우
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponseDto> handleMissingParams(MissingServletRequestParameterException e) {
		String name = e.getParameterName();
		log.error("Required request parameter '{}' is not present", name);
		String message = String.format("필수 파라미터인 '%s'를 포함해야 합니다.", name);
		ErrorResponseDto errorResponse = new ErrorResponseDto(message, HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	/**
	 * 사전답사 중복 예외 처리 (409 Conflict)
	 */
	@ExceptionHandler(PrevisitAlreadyExistsException.class)
	public ResponseEntity<ErrorResponseDto> handlePrevisitAlreadyExists(PrevisitAlreadyExistsException e) {
		log.error("Previsit already exists error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.CONFLICT.value());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
	}

	/**
	 * 접근 권한 없음 예외 처리 (403 Forbidden)
	 * 다른 사용자의 예약에 접근하거나 권한이 없는 경우
	 */
	@ExceptionHandler(ForbiddenAccessException.class)
	public ResponseEntity<ErrorResponseDto> handleForbiddenAccess(ForbiddenAccessException e) {
		log.error("Forbidden Access error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.FORBIDDEN.value());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
	}

	/**
	 * 잘못된 입력값 예외 처리 (400 Bad Request)
	 * 유효하지 않은 날짜, 시간 형식 등
	 */
	@ExceptionHandler(InvalidInputValueException.class)
	public ResponseEntity<ErrorResponseDto> handleInvalidInputValue(InvalidInputValueException e) {
		log.error("Invalid Input Value error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	/**
	 * 리소스 없음 예외 처리 (404 Not Found)
	 * 존재하지 않는 예약, 사용자, 공간 등
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException e) {
		log.error("Resource Not Found error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.NOT_FOUND.value());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
	}

	/**
	 * 이메일 중복 예외 처리 (400 Bad Request)
	 */
	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<ErrorResponseDto> handleDuplicateEmail(DuplicateEmailException e) {
		log.error("Duplicate email error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * 회사 없음 예외 처리 (400 Bad Request)
	 */
	@ExceptionHandler(CompanyNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleCompanyNotFound(CompanyNotFoundException e) {
		log.error("Company not found error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * 사용자 등록 예외 처리 (500 Internal Server Error)
	 */
	@ExceptionHandler(UserRegistrationException.class)
	public ResponseEntity<ErrorResponseDto> handleUserRegistration(UserRegistrationException e) {
		log.error("User registration error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	/**
	 * 외부 API 호출 실패 예외 처리 (502 Bad Gateway)
	 * CoolSMS, OpenAI 등 외부 API 호출 실패
	 */
	@ExceptionHandler(ExternalApiException.class)
	public ResponseEntity<ErrorResponseDto> handleExternalApiException(ExternalApiException e) {
		log.error("External API error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.BAD_GATEWAY.value());
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
	}

	/**
	 * DTO 검증 실패 예외 처리 (400 Bad Request)
	 * @Valid 검증 실패 시 필드별 에러 메시지 반환
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationExceptions(
		MethodArgumentNotValidException ex) {

		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError)error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		log.error("Validation error: {}", errors);
		return ResponseEntity.badRequest().body(errors);
	}

	/**
	 * 바인딩 예외 처리 (400 Bad Request)
	 * 폼 데이터 바인딩 실패 시
	 */
	@ExceptionHandler(BindException.class)
	public ResponseEntity<Map<String, String>> handleBindException(BindException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError)error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		log.error("Bind error: {}", errors);
		return ResponseEntity.badRequest().body(errors);
	}

	/**
	 * 예기치 않은 예외 처리 (500 Internal Server Error)
	 * 모든 처리되지 않은 예외의 최종 처리
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDto> handleGeneral(Exception e) {
		log.error("Unexpected error: ", e);
		ErrorResponseDto errorResponse = new ErrorResponseDto(
			"서버 내부 오류가 발생했습니다.",
			HttpStatus.INTERNAL_SERVER_ERROR.value()
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	/**
	 * JSON 역직렬화 실패 예외 처리 (400 Bad Request)
	 * LocalTime, Enum 등의 형식이 잘못된 경우
	 */
	@ExceptionHandler(com.fasterxml.jackson.databind.exc.InvalidFormatException.class)
	public ResponseEntity<?> handleInvalidFormat(InvalidFormatException ex) {
		return ResponseEntity.badRequest().body(Map.of(
			"message", "입력 형식이 올바르지 않습니다.",
			"detail", ex.getOriginalMessage()
		));
	}

	/**
	 * HTTP 메시지 읽기 실패 예외 처리 (400 Bad Request)
	 * 요청 본문의 JSON이 잘못된 경우
	 */
	@ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
		var cause = ex.getCause();
		if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife) {
			return handleInvalidFormat(ife);
		}
		return ResponseEntity.badRequest().body(Map.of(
			"message", "요청 본문을 읽을 수 없습니다.",
			"detail", ex.getMessage()
		));
	}

	/**
	 * 제약 조건 위반 예외 처리 (400 Bad Request)
	 * @RequestParam, @PathVariable 검증 실패
	 * 컨트롤러에 @Validated 필요
	 */
	@ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
	public ResponseEntity<?> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
		var errors = ex.getConstraintViolations().stream()
			.map(v -> v.getPropertyPath() + ": " + v.getMessage())
			.toList();
		return ResponseEntity.badRequest().body(Map.of(
			"message", "요청 파라미터 검증 실패",
			"errors", errors
		));
	}

	/**
	 * 잘못된 인자 예외 처리 (400 Bad Request)
	 * 서비스 레이어에서 발생한 IllegalArgumentException
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
	}

	/**
	 * ResponseStatusException 처리
	 * 서비스 계층의 비즈니스 로직과 HTTP 응답을 연결하는 브릿지
	 */
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {
		return new ResponseEntity<>(ex.getReason(), ex.getStatusCode());
	}
}
