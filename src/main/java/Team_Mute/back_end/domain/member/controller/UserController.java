package Team_Mute.back_end.domain.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Team_Mute.back_end.domain.member.dto.request.PasswordResetRequestDto;
import Team_Mute.back_end.domain.member.dto.request.PasswordUpdateRequestDto;
import Team_Mute.back_end.domain.member.dto.request.SignupRequestDto;
import Team_Mute.back_end.domain.member.dto.request.UserInfoUpdateRequestDto;
import Team_Mute.back_end.domain.member.dto.response.SignupResponseDto;
import Team_Mute.back_end.domain.member.dto.response.UserInfoResponseDto;
import Team_Mute.back_end.domain.member.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "사용자 API", description = "사용자 관련 API 명세")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Slf4j
public class UserController {

	private final UserService userService;

	@Operation(summary = "사용자 회원가입", description = "사용자 정보를 받아 회원가입을 진행합니다.")
	@PostMapping("/signup")
	public ResponseEntity<SignupResponseDto> signUp(@Valid @RequestBody SignupRequestDto requestDto) {
		log.info("회원가입 API 호출: {}", requestDto.getUserEmail());

		SignupResponseDto response = userService.signUp(requestDto);

		log.info("회원가입 API 응답: {}", response.getMessage());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "이메일 중복 체크", description = "이미 DB에 저장된 이메일인지 확인합니다.")
	@GetMapping("/check-email")
	public ResponseEntity<Boolean> checkEmailDuplication(@RequestParam String email) {
		log.info("이메일 중복 체크: {}", email);

		boolean exists = userService.isEmailExists(email);
		return ResponseEntity.ok(exists);
	}

	@Operation(summary = "사용자 정보 조회", description = "토큰을 확인하여 사용자 정보를 조회합니다.")
	@GetMapping("/account")
	public ResponseEntity<UserInfoResponseDto> getUserInfo(@AuthenticationPrincipal String userId) {
		log.info("회원 정보 조회 API 호출: userId={}", userId);
		UserInfoResponseDto userInfo = userService.getUserInfo(Long.parseLong(userId));
		return ResponseEntity.ok(userInfo);
	}

	@Operation(summary = "사용자 정보 수정", description = "토큰과 수정할 사용자 정보를 받아 수정합니다.")
	@PutMapping("/account")
	public ResponseEntity<String> updateUserInfo(@AuthenticationPrincipal String userId,
		@Valid @RequestBody UserInfoUpdateRequestDto requestDto) {
		log.info("회원 정보 수정 API 호출: userId={}", userId);
		userService.updateUserInfo(Long.parseLong(userId), requestDto);
		return ResponseEntity.ok("회원 정보가 성공적으로 수정되었습니다.");
	}

	@Operation(summary = "사용자 탈퇴", description = "토큰을 확인하여 해당 사용자를 탈퇴시킵니다.")
	@DeleteMapping("/account")
	public ResponseEntity<String> deleteUser(@AuthenticationPrincipal String userId) {
		log.info("회원 탈퇴 API 호출: userId={}", userId);
		userService.deleteUser(Long.parseLong(userId));
		return ResponseEntity.ok("회원 탈퇴가 성공적으로 처리되었습니다.");
	}

	@Operation(summary = "사용자 비밀번호 수정", description = "토큰과 기존 비밀번호, 신규 비밀번호를 받아 비밀번호를 수정합니다.")
	@PutMapping("/account/password")
	public ResponseEntity<String> updatePassword(@AuthenticationPrincipal String userId,
		@Valid @RequestBody PasswordUpdateRequestDto requestDto) {
		log.info("비밀번호 수정 API 호출: userId={}", userId);
		userService.updatePassword(Long.parseLong(userId), requestDto);
		return ResponseEntity.ok("비밀번호가 성공적으로 수정되었습니다.");
	}

	@Operation(summary = "사용자 비밀번호 초기화", description = "이메일을 받아 해당 이메일로 임시비밀번호를 발송합니다.")
	@PostMapping("/reset-password")
	public ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetRequestDto requestDto) {
		log.info("비밀번호 초기화 API 호출: email={}", requestDto.getUserEmail());
		userService.resetPassword(requestDto);
		return ResponseEntity.ok("임시 비밀번호를 발송드렸습니다. 이메일을 확인해주세요");
	}
}
