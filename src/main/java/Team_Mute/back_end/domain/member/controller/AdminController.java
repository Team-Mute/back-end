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
import Team_Mute.back_end.domain.member.dto.response.AdminSignupResponseDto;
import Team_Mute.back_end.domain.member.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 계정 관리 관련 요청을 처리하는 컨트롤러입니다.
 * 관리자 회원가입, 정보 조회/수정/삭제, 비밀번호 관리, 권한 관리 기능을 제공
 * 일부 기능은 마스터 관리자(roleId=0) 권한이 필요하며, 서비스 레이어에서 권한 검증 수행
 * JWT 인증을 통해 현재 로그인한 관리자의 ID를 추출하여 사용
 *
 * @author Team Mute
 * @since 1.0
 */
@Tag(name = "관리자 API", description = "관리자 관련 API 명세")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

	private final AdminService userService;

	/**
	 * 관리자 회원가입
	 * - 마스터 관리자 권한으로 새로운 관리자 계정을 생성
	 * - 이메일 중복 검증 및 비밀번호 암호화 처리
	 * - 관리자 역할(1차 승인자, 2차 승인자, 마스터 관리자) 및 담당 지역 설정
	 * - 회원가입 성공 시 생성된 관리자 정보 반환
	 * - 마스터 관리자만 접근 가능하며, 서비스 레이어에서 권한 검증
	 *
	 * @param requestDto 회원가입 요청 DTO (이메일, 비밀번호, 이름, 전화번호, 역할, 담당 지역 포함)
	 * @return 생성된 관리자 정보를 포함하는 {@code ResponseEntity<AdminSignupResponseDto>} (201 Created)
	 */
	@Operation(summary = "관리자 회원가입", description = "마스터 관리자 권한으로 관리자 계정을 생성합니다.")
	@PostMapping("/signup")
	public ResponseEntity<AdminSignupResponseDto> adminSignUp(
		@Valid @RequestBody AdminSignupRequestDto requestDto
	) {
		AdminSignupResponseDto responseDto = userService.adminSignUp(requestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
	}

	/**
	 * 관리자 정보 조회
	 * - JWT 토큰에서 추출한 관리자 ID를 기반으로 본인의 계정 정보 조회
	 * - 이메일, 이름, 전화번호, 권한, 담당 지역 정보 반환
	 * - 인증된 관리자만 접근 가능하며, 본인의 정보만 조회 가능
	 *
	 * @param authentication Spring Security의 인증 컨텍스트 (관리자 ID 포함)
	 * @return 관리자 상세 정보를 포함하는 {@code ResponseEntity<AdminInfoResponse>}
	 */
	@Operation(summary = "관리자 정보 조회", description = "토큰을 확인하여 관리자 정보를 조회합니다.")
	@GetMapping("/account")
	public ResponseEntity<AdminInfoResponse> getAdminInfo(Authentication authentication) {
		Long adminId = Long.valueOf((String)authentication.getPrincipal());
		AdminInfoResponse response = userService.getAdminInfo(adminId);
		return ResponseEntity.ok(response);
	}

	/**
	 * 관리자 정보 수정
	 * - JWT 토큰에서 추출한 관리자 ID를 기반으로 본인의 계정 정보 수정
	 * - 기본 정보(이메일, 이름, 전화번호) 수정 가능
	 * - 권한은 별도 API를 통해 수정
	 * - 인증된 관리자만 본인의 정보를 수정 가능
	 *
	 * @param authentication Spring Security의 인증 컨텍스트 (관리자 ID 포함)
	 * @param requestDto 수정할 관리자 정보 DTO (이메일, 이름, 전화번호)
	 * @return 수정 완료 메시지를 포함하는 {@code ResponseEntity<String>}
	 */
	@Operation(summary = "관리자 정보 수정", description = "토큰과 수정할 관리자 정보를 받아 정보를 수정합니다.")
	@PutMapping("/account")
	public ResponseEntity<String> updateAdminInfo(
		Authentication authentication,
		@Valid @RequestBody AdminAccountUpdateRequest requestDto
	) {
		Long adminId = Long.valueOf((String)authentication.getPrincipal());
		userService.updateAdminInfo(adminId, requestDto);
		return ResponseEntity.ok("회원 정보가 성공적으로 수정되었습니다.");
	}

	/**
	 * 관리자 계정 삭제
	 * - 마스터 관리자 권한으로 특정 관리자 계정을 삭제
	 * - 삭제 대상 관리자의 이메일을 요청 바디로 전달
	 * - 마스터 관리자만 접근 가능하며, 서비스 레이어에서 권한 검증
	 * - 자기 자신은 삭제할 수 없도록 제한
	 * - 삭제된 관리자와 연관된 데이터 처리 로직 포함
	 *
	 * @param authentication Spring Security의 인증 컨텍스트 (현재 로그인한 관리자 ID 포함)
	 * @param requestDto 삭제할 관리자 이메일 정보 DTO
	 * @return 삭제 완료 메시지를 포함하는 {@code ResponseEntity<String>}
	 */
	@Operation(summary = "관리자 삭제", description = "마스터 관리자 권한으로 관리자 계정을 삭제합니다.")
	@DeleteMapping("/account")
	public ResponseEntity<String> deleteAdminAccount(
		Authentication authentication,
		@Valid @RequestBody AdminAccountDeleteRequest requestDto
	) {
		userService.deleteAdminAccount(authentication, requestDto);
		return ResponseEntity.ok("회원이 성공적으로 삭제되었습니다.");
	}

	/**
	 * 관리자 비밀번호 수정
	 * - JWT 토큰에서 추출한 관리자 ID를 기반으로 본인의 비밀번호 수정
	 * - 기존 비밀번호를 검증한 후 새로운 비밀번호로 변경
	 * - 새 비밀번호는 BCrypt 알고리즘으로 암호화하여 저장
	 * - 비밀번호 정책(최소 길이, 특수문자 포함 등) 검증
	 * - 인증된 관리자만 본인의 비밀번호를 수정 가능
	 *
	 * @param authentication Spring Security의 인증 컨텍스트 (관리자 ID 포함)
	 * @param requestDto 비밀번호 수정 요청 DTO (기존 비밀번호, 새 비밀번호 포함)
	 * @return 수정 완료 메시지를 포함하는 {@code ResponseEntity<String>}
	 */
	@Operation(summary = "관리자 비밀번호 수정", description = "토큰과 기존 비밀번호, 신규 비밀번호를 받아 비밀번호를 수정합니다.")
	@PutMapping("/account/password")
	public ResponseEntity<String> updateAdminPassword(
		Authentication authentication,
		@Valid @RequestBody AdminPasswordUpdateRequest requestDto
	) {
		Long adminId = Long.valueOf((String)authentication.getPrincipal());
		userService.updateAdminPassword(adminId, requestDto);
		return ResponseEntity.ok("비밀번호가 성공적으로 수정되었습니다.");
	}

	/**
	 * 관리자 비밀번호 초기화
	 * - 비밀번호를 잊어버린 관리자를 위한 비밀번호 초기화 기능
	 * - 이메일 주소를 입력받아 해당 계정의 임시 비밀번호 생성
	 * - 생성된 임시 비밀번호를 이메일로 발송
	 * - 임시 비밀번호는 BCrypt로 암호화하여 DB에 저장
	 * - 로그인 후 비밀번호 변경 권장
	 * - 인증 없이 접근 가능한 공개 API
	 *
	 * @param requestDto 비밀번호 초기화 요청 DTO (이메일 주소 포함)
	 * @return 임시 비밀번호 발송 완료 메시지를 포함하는 {@code ResponseEntity<String>}
	 */
	@Operation(summary = "관리자 비밀번호 초기화", description = "이메일을 받아 해당 이메일로 임시 비밀번호를 발송합니다.")
	@PostMapping("/reset-password")
	public ResponseEntity<String> resetAdminPassword(
		@Valid @RequestBody AdminPasswordResetRequest requestDto
	) {
		userService.resetAdminPassword(requestDto);
		return ResponseEntity.ok("임시 비밀번호를 발송했습니다. 이메일을 확인해주세요.");
	}

	/**
	 * 관리자 권한 수정
	 * - 마스터 관리자 권한으로 특정 관리자의 역할 및 담당 지역 수정
	 * - roleId 변경: 2차 승인자(1), 1차 승인자(2), 마스터 관리자(0) 간 전환
	 * - 담당 지역 변경: 관리자가 관리하는 지역 범위 수정
	 * - 마스터 관리자만 접근 가능하며, 서비스 레이어에서 권한 검증
	 * - 자기 자신의 권한은 변경할 수 없도록 제한
	 *
	 * @param authentication Spring Security의 인증 컨텍스트 (현재 로그인한 관리자 ID 포함)
	 * @param requestDto 권한 수정 요청 DTO (대상 관리자 ID, 새로운 roleId, 담당 지역 포함)
	 * @return 권한 수정 완료 메시지를 포함하는 {@code ResponseEntity<String>}
	 */
	@Operation(summary = "관리자 권한 수정", description = "마스터 관리자 권한으로 관리자 권한을 수정합니다.")
	@PutMapping("/account/roles")
	public ResponseEntity<String> updateAdminRole(
		Authentication authentication,
		@Valid @RequestBody AdminRoleUpdateRequest requestDto
	) {
		userService.updateAdminRole(authentication, requestDto);
		return ResponseEntity.ok("회원 권한이 성공적으로 수정되었습니다.");
	}
}
