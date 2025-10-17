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

import Team_Mute.back_end.domain.member.dto.response.ErrorResponseDto;
import Team_Mute.back_end.domain.member.exception.CompanyNotFoundException;
import Team_Mute.back_end.domain.member.exception.DuplicateEmailException;
import Team_Mute.back_end.domain.member.exception.ExternalApiException;
import Team_Mute.back_end.domain.member.exception.UserRegistrationException;
import Team_Mute.back_end.domain.reservation.exception.ForbiddenAccessException;
import Team_Mute.back_end.domain.reservation.exception.InvalidInputValueException;
import Team_Mute.back_end.domain.reservation.exception.PrevisitAlreadyExistsException;
import Team_Mute.back_end.domain.reservation.exception.ReservationConflictException;
import Team_Mute.back_end.domain.reservation.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	@ExceptionHandler(ReservationConflictException.class)
	public ResponseEntity<ErrorResponseDto> handleReservationConflict(ReservationConflictException ex) {
		ErrorResponseDto errorResponse = new ErrorResponseDto(
			ex.getMessage(),
			HttpStatus.CONFLICT.value(),
			LocalDateTime.now()
		);

		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponseDto> handleHttpRequestMethodNotSupported(
		HttpRequestMethodNotSupportedException e) {
		log.error("Method Not Allowed: {} {} - Supported methods are {}", e.getMethod(), e.getLocalizedMessage(),
			e.getSupportedMethods());

		String supportedMethods = String.join(", ", Objects.requireNonNull(e.getSupportedMethods()));
		String message = String.format("해당 경로는 %s 메서드를 지원하지 않습니다. (지원하는 메서드: %s)", e.getMethod(), supportedMethods);

		ErrorResponseDto errorResponse = new ErrorResponseDto(message, HttpStatus.METHOD_NOT_ALLOWED.value());

		// HTTP 405 응답 시에는 헤더에 'Allow'를 포함해주는 것이 표준입니다.
		return ResponseEntity
			.status(HttpStatus.METHOD_NOT_ALLOWED)
			.header("Allow", supportedMethods)
			.body(errorResponse);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponseDto> handleMissingParams(MissingServletRequestParameterException e) {
		String name = e.getParameterName();
		log.error("Required request parameter '{}' is not present", name);
		String message = String.format("필수 파라미터인 '%s'를 포함해야 합니다.", name);
		ErrorResponseDto errorResponse = new ErrorResponseDto(message, HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	@ExceptionHandler(PrevisitAlreadyExistsException.class)
	public ResponseEntity<ErrorResponseDto> handlePrevisitAlreadyExists(PrevisitAlreadyExistsException e) {
		log.error("Previsit already exists error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.CONFLICT.value());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
	}

	@ExceptionHandler(ForbiddenAccessException.class)
	public ResponseEntity<ErrorResponseDto> handleForbiddenAccess(ForbiddenAccessException e) {
		log.error("Forbidden Access error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.FORBIDDEN.value());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
	}

	@ExceptionHandler(InvalidInputValueException.class)
	public ResponseEntity<ErrorResponseDto> handleInvalidInputValue(InvalidInputValueException e) {
		log.error("Invalid Input Value error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException e) {
		log.error("Resource Not Found error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.NOT_FOUND.value());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
	}

	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<ErrorResponseDto> handleDuplicateEmail(DuplicateEmailException e) {
		log.error("Duplicate email error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.badRequest().body(errorResponse);
	}

	@ExceptionHandler(CompanyNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleCompanyNotFound(CompanyNotFoundException e) {
		log.error("Company not found error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.badRequest().body(errorResponse);
	}

	@ExceptionHandler(UserRegistrationException.class)
	public ResponseEntity<ErrorResponseDto> handleUserRegistration(UserRegistrationException e) {
		log.error("User registration error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	@ExceptionHandler(ExternalApiException.class)
	public ResponseEntity<ErrorResponseDto> handleExternalApiException(ExternalApiException e) {
		log.error("External API error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.BAD_GATEWAY.value());
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
	}

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

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDto> handleGeneral(Exception e) {
		log.error("Unexpected error: ", e);
		ErrorResponseDto errorResponse = new ErrorResponseDto(
			"서버 내부 오류가 발생했습니다.",
			HttpStatus.INTERNAL_SERVER_ERROR.value()
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	// LocalTime/Enum 등 역직렬화 실패 (포장 X)
	@ExceptionHandler(com.fasterxml.jackson.databind.exc.InvalidFormatException.class)
	public ResponseEntity<?> handleInvalidFormat(InvalidFormatException ex) {
		return ResponseEntity.badRequest().body(Map.of(
			"message", "입력 형식이 올바르지 않습니다.",
			"detail", ex.getOriginalMessage()
		));
	}

	// 역직렬화 실패 (포장 O: HttpMessageNotReadableException)
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

	// @RequestParam/@PathVariable 검증 실패 (컨트롤러에 @Validated 필요)
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

	// 서비스 가드
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
	}

	// 서비스 계층의 비즈니스 로직(예외 발생)과 HTTP 응답(클라이언트에게 전달)을 자연스럽게 연결해주는 "다리" 역할
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {
		return new ResponseEntity<>(ex.getReason(), ex.getStatusCode());
	}
}
