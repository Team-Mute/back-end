package Team_Mute.back_end.domain.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Team_Mute.back_end.domain.member.dto.request.AdminAccountDeleteRequest;
import Team_Mute.back_end.domain.member.dto.request.AdminAccountUpdateRequest;
import Team_Mute.back_end.domain.member.dto.request.AdminPasswordResetRequest;
import Team_Mute.back_end.domain.member.dto.request.AdminPasswordUpdateRequest;
import Team_Mute.back_end.domain.member.dto.request.AdminRoleUpdateRequest;
import Team_Mute.back_end.domain.member.dto.request.AdminSignupRequestDto;
import Team_Mute.back_end.domain.member.dto.response.AdminInfoResponse;
import Team_Mute.back_end.domain.member.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "관리자 API", description = "관리자 관련 API 명세")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

	private final UserService userService;

	@Operation(summary = "관리자 회원가입", description = "마스터 관리자 권한으로 관리자 계정을 생성합니다.")
	@PostMapping("/signup")
	public ResponseEntity<String> adminSignUp(@Valid @RequestBody AdminSignupRequestDto requestDto) {
		log.info("관리자용 회원가입 API 호출: email={}", requestDto.getUserEmail());
		userService.adminSignUp(requestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
	}

	@Operation(summary = "관리자 정보 조회", description = "토큰을 확인하여 관리자 정보를 조회합니다.")
	@GetMapping("/account")
	public ResponseEntity<AdminInfoResponse> getAdminInfo(Authentication authentication) {
		Long adminId = Long.valueOf((String)authentication.getPrincipal());
		AdminInfoResponse response = userService.getAdminInfo(adminId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "관리자 정보 수정", description = "토큰과 수정할 관리자 정보를 받아 정보를 수정합니다.")
	@PutMapping("/account")
	public ResponseEntity<String> updateAdminInfo(Authentication authentication,
		@Valid @RequestBody AdminAccountUpdateRequest requestDto) {
		Long adminId = Long.valueOf((String)authentication.getPrincipal());
		userService.updateAdminInfo(adminId, requestDto);
		return ResponseEntity.ok("회원 정보가 성공적으로 수정되었습니다.");
	}

	@Operation(summary = "관리자 삭제", description = "마스터 관리자 권한으로 관리자 계정을 삭제합니다.")
	@DeleteMapping("/account")
	public ResponseEntity<String> deleteAdminAccount(Authentication authentication,
		@Valid @RequestBody AdminAccountDeleteRequest requestDto) {
		userService.deleteAdminAccount(authentication, requestDto);
		return ResponseEntity.ok("회원이 성공적으로 삭제되었습니다.");
	}

	@Operation(summary = "관리자 비밀번호 수정", description = "토큰과 기존 비밀번호, 신규 비밀번호를 받아 비밀번호를 수정합니다.")
	@PutMapping("/account/password")
	public ResponseEntity<String> updateAdminPassword(Authentication authentication,
		@Valid @RequestBody AdminPasswordUpdateRequest requestDto) {
		Long adminId = Long.valueOf((String)authentication.getPrincipal());
		userService.updateAdminPassword(adminId, requestDto);
		return ResponseEntity.ok("비밀번호가 성공적으로 수정되었습니다.");
	}

	@Operation(summary = "관리자 비밀번호 초기화", description = "이메일을 받아 해당 이메일로 임시 비밀번호를 발송합니다.")
	@PostMapping("/reset-password")
	public ResponseEntity<String> resetAdminPassword(@Valid @RequestBody AdminPasswordResetRequest requestDto) {
		userService.resetAdminPassword(requestDto);
		return ResponseEntity.ok("임시 비밀번호를 발송했습니다. 이메일을 확인해주세요.");
	}

	@Operation(summary = "관리자 권한 수정", description = "마스터 관리자 권한으로 관리자 권한을 수정합니다.")
	@PutMapping("/account/roles")
	public ResponseEntity<String> updateAdminRole(Authentication authentication,
		@Valid @RequestBody AdminRoleUpdateRequest requestDto) {
		userService.updateAdminRole(authentication, requestDto);
		return ResponseEntity.ok("회원 권한이 성공적으로 수정되었습니다.");
	}

}
