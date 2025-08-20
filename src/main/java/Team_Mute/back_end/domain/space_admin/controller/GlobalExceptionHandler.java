package Team_Mute.back_end.domain.space_admin.controller;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice // = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

	// DTO(@Valid) 검증 실패
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
		var errors = ex.getBindingResult().getFieldErrors().stream()
			.collect(Collectors.groupingBy(
				FieldError::getField,
				Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
			));
		return ResponseEntity.badRequest().body(Map.of(
			"message", "요청 검증 실패",
			"errors", errors
		));
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
