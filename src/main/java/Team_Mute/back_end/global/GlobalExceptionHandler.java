package Team_Mute.back_end.global;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import Team_Mute.back_end.domain.member.dto.response.ErrorResponseDto;
import Team_Mute.back_end.domain.member.exception.CompanyNotFoundException;
import Team_Mute.back_end.domain.member.exception.DuplicateEmailException;
import Team_Mute.back_end.domain.member.exception.ExternalApiException;
import Team_Mute.back_end.domain.member.exception.UserRegistrationException;
import Team_Mute.back_end.domain.smsAuth.exception.InvalidVerificationException;
import Team_Mute.back_end.domain.smsAuth.exception.SmsSendingFailedException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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
}
