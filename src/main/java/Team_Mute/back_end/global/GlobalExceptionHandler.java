package Team_Mute.back_end.global;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import Team_Mute.back_end.domain.member.dto.response.ErrorResponseDto;
import Team_Mute.back_end.domain.member.exception.CompanyNotFoundException;
import Team_Mute.back_end.domain.member.exception.DuplicateEmailException;
import Team_Mute.back_end.domain.member.exception.ExternalApiException;
import Team_Mute.back_end.domain.member.exception.UserRegistrationException;
import Team_Mute.back_end.domain.reservation.exception.ForbiddenAccessException;
import Team_Mute.back_end.domain.reservation.exception.InvalidInputValueException;
import Team_Mute.back_end.domain.reservation.exception.ResourceNotFoundException;
import Team_Mute.back_end.domain.smsAuth.exception.InvalidVerificationException;
import Team_Mute.back_end.domain.smsAuth.exception.SmsSendingFailedException;
import lombok.extern.slf4j.Slf4j;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
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

	@ExceptionHandler(SmsSendingFailedException.class)
	public ResponseEntity<ErrorResponseDto> handleSmsSendingFailed(SmsSendingFailedException e) {
		log.error("Sms sending failed error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	@ExceptionHandler(InvalidVerificationException.class)
	public ResponseEntity<ErrorResponseDto> handleInvalidVerification(InvalidVerificationException e) {
		log.error("Invalid verification error: {}", e.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(e.getMessage(), HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.badRequest().body(errorResponse);
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
}
