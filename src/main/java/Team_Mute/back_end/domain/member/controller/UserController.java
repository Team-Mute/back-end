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

/**
 * 사용자 계정 관리 관련 요청을 처리하는 컨트롤러입니다.
 * 사용자 회원가입, 정보 조회/수정/삭제, 비밀번호 관리, 이메일 중복 체크 기능을 제공
 * JWT 인증을 통해 현재 로그인한 사용자의 ID를 추출하여 본인 정보만 접근 가능하도록 제어
 * 회원가입과 이메일 중복 체크, 비밀번호 초기화는 인증 없이 접근 가능한 공개 API
 * 비밀번호는 BCrypt로 암호화하여 저장하며, Token Version 시스템으로 비밀번호 변경 시 기존 토큰 무효화
 *
 * @author Team Mute
 * @since 1.0
 */
@Tag(name = "사용자 API", description = "사용자 관련 API 명세")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Slf4j
public class UserController {

	private final UserService userService;

	/**
	 * 사용자 회원가입
	 * - 이메일, 비밀번호, 이름, 소속 기업 정보로 새로운 사용자 계정 생성
	 * - 이메일 중복 검증 및 비밀번호 암호화(BCrypt) 처리
	 * - 소속 기업 정보는 CorpInfoController에서 검색한 결과를 사용
	 * - 회원가입 성공 시 초기 Token Version(0) 설정
	 * - 인증 없이 접근 가능한 공개 API
	 * - 회원가입 완료 후 로그인 필요
	 *
	 * @param requestDto 회원가입 요청 DTO (이메일, 비밀번호, 이름, 회사 정보 포함)
	 * @return 회원가입 성공 메시지를 포함하는 {@code ResponseEntity<SignupResponseDto>} (201 Created)
	 */
	@Operation(summary = "사용자 회원가입", description = "사용자 정보를 받아 회원가입을 진행합니다.")
	@PostMapping("/signup")
	public ResponseEntity<SignupResponseDto> signUp(
		@Valid @RequestBody SignupRequestDto requestDto
	) {
		SignupResponseDto response = userService.signUp(requestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 이메일 중복 체크
	 * - 입력된 이메일이 이미 데이터베이스에 등록되어 있는지 확인
	 * - 회원가입 시 실시간 이메일 중복 검증에 사용
	 * - 프론트엔드에서 사용자가 이메일을 입력하는 동안 비동기로 호출
	 * - true: 이메일 존재(중복), false: 이메일 미존재(사용 가능)
	 * - 인증 없이 접근 가능한 공개 API
	 *
	 * @param email 중복 확인할 이메일 주소 (쿼리 파라미터)
	 * @return 이메일 존재 여부를 포함하는 {@code ResponseEntity<Boolean>}
	 */
	@Operation(summary = "이메일 중복 체크", description = "이미 DB에 저장된 이메일인지 확인합니다.")
	@GetMapping("/check-email")
	public ResponseEntity<Boolean> checkEmailDuplication(@RequestParam String email) {
		boolean exists = userService.isEmailExists(email);
		return ResponseEntity.ok(exists);
	}

	/**
	 * 사용자 정보 조회
	 * - JWT 토큰에서 추출한 사용자 ID를 기반으로 본인의 계정 정보 조회
	 * - 이메일, 이름, 소속 기업, 역할 등의 정보 반환
	 * - @AuthenticationPrincipal을 통해 Spring Security 컨텍스트에서 사용자 ID 자동 주입
	 * - 인증된 사용자만 본인의 정보를 조회 가능
	 * - 마이페이지, 프로필 화면에서 사용
	 *
	 * @param userId JWT 토큰에서 추출된 사용자 ID (자동 주입)
	 * @return 사용자 상세 정보를 포함하는 {@code ResponseEntity<UserInfoResponseDto>}
	 */
	@Operation(summary = "사용자 정보 조회", description = "토큰을 확인하여 사용자 정보를 조회합니다.")
	@GetMapping("/account")
	public ResponseEntity<UserInfoResponseDto> getUserInfo(
		@AuthenticationPrincipal String userId
	) {
		UserInfoResponseDto userInfo = userService.getUserInfo(Long.parseLong(userId));
		return ResponseEntity.ok(userInfo);
	}

	/**
	 * 사용자 정보 수정
	 * - JWT 토큰에서 추출한 사용자 ID를 기반으로 본인의 계정 정보 수정
	 * - 기본 정보(이름, 이메일) 수정 가능
	 * - 비밀번호는 별도 API를 통해 수정
	 * - @AuthenticationPrincipal을 통해 사용자 ID 자동 주입
	 * - 인증된 사용자만 본인의 정보를 수정 가능
	 * - 마이페이지 정보 수정 기능에서 사용
	 *
	 * @param userId JWT 토큰에서 추출된 사용자 ID (자동 주입)
	 * @param requestDto 수정할 사용자 정보 DTO (이름, 전화번호 등)
	 * @return 수정 완료 메시지를 포함하는 {@code ResponseEntity<String>}
	 */
	@Operation(summary = "사용자 정보 수정", description = "토큰과 수정할 사용자 정보를 받아 수정합니다.")
	@PutMapping("/account")
	public ResponseEntity<String> updateUserInfo(
		@AuthenticationPrincipal String userId,
		@Valid @RequestBody UserInfoUpdateRequestDto requestDto
	) {
		userService.updateUserInfo(Long.parseLong(userId), requestDto);
		return ResponseEntity.ok("회원 정보가 성공적으로 수정되었습니다.");
	}

	/**
	 * 사용자 탈퇴
	 * - JWT 토큰에서 추출한 사용자 ID를 기반으로 본인의 계정 삭제
	 * - 사용자 탈퇴 시 연관된 예약 정보, 로그인 이력 등 처리 로직 포함
	 * - @AuthenticationPrincipal을 통해 사용자 ID 자동 주입
	 * - 인증된 사용자만 본인의 계정을 탈퇴 가능
	 * - 탈퇴 후 재가입 정책은 서비스 레이어에서 관리
	 *
	 * @param userId JWT 토큰에서 추출된 사용자 ID (자동 주입)
	 * @return 탈퇴 완료 메시지를 포함하는 {@code ResponseEntity<String>}
	 */
	@Operation(summary = "사용자 탈퇴", description = "토큰을 확인하여 해당 사용자를 탈퇴시킵니다.")
	@DeleteMapping("/account")
	public ResponseEntity<String> deleteUser(@AuthenticationPrincipal String userId) {
		log.info("회원 탈퇴 API 호출: userId={}", userId);
		userService.deleteUser(Long.parseLong(userId));
		return ResponseEntity.ok("회원 탈퇴가 성공적으로 처리되었습니다.");
	}

	/**
	 * 사용자 비밀번호 수정
	 * - JWT 토큰에서 추출한 사용자 ID를 기반으로 본인의 비밀번호 수정
	 * - 기존 비밀번호를 검증한 후 새로운 비밀번호로 변경
	 * - 새 비밀번호는 BCrypt 알고리즘으로 암호화하여 저장
	 * - Token Version을 증가시켜 비밀번호 변경 전 발급된 모든 JWT 토큰 무효화 (보안 강화)
	 * - 비밀번호 정책(최소 길이, 특수문자 포함 등) 검증
	 * - 인증된 사용자만 본인의 비밀번호를 수정 가능
	 * - 비밀번호 변경 후 재로그인 필요
	 *
	 * @param userId JWT 토큰에서 추출된 사용자 ID (자동 주입)
	 * @param requestDto 비밀번호 수정 요청 DTO (기존 비밀번호, 새 비밀번호 포함)
	 * @return 수정 완료 메시지를 포함하는 {@code ResponseEntity<String>}
	 */
	@Operation(summary = "사용자 비밀번호 수정", description = "토큰과 기존 비밀번호, 신규 비밀번호를 받아 비밀번호를 수정합니다.")
	@PutMapping("/account/password")
	public ResponseEntity<String> updatePassword(
		@AuthenticationPrincipal String userId,
		@Valid @RequestBody PasswordUpdateRequestDto requestDto
	) {
		userService.updatePassword(Long.parseLong(userId), requestDto);
		return ResponseEntity.ok("비밀번호가 성공적으로 수정되었습니다.");
	}

	/**
	 * 사용자 비밀번호 초기화
	 * - 비밀번호를 잊어버린 사용자를 위한 비밀번호 초기화 기능
	 * - 이메일 주소를 입력받아 해당 계정의 임시 비밀번호 생성
	 * - 생성된 임시 비밀번호를 EmailService를 통해 이메일로 발송
	 * - 임시 비밀번호는 BCrypt로 암호화하여 DB에 저장
	 * - Token Version을 증가시켜 비밀번호 초기화 전 발급된 모든 JWT 토큰 무효화
	 * - 로그인 후 비밀번호 변경 권장
	 * - 인증 없이 접근 가능한 공개 API
	 *
	 * @param requestDto 비밀번호 초기화 요청 DTO (이메일 주소 포함)
	 * @return 임시 비밀번호 발송 완료 메시지를 포함하는 {@code ResponseEntity<String>}
	 */
	@Operation(summary = "사용자 비밀번호 초기화", description = "이메일을 받아 해당 이메일로 임시비밀번호를 발송합니다.")
	@PostMapping("/reset-password")
	public ResponseEntity<String> resetPassword(
		@Valid @RequestBody PasswordResetRequestDto requestDto
	) {
		userService.resetPassword(requestDto);
		return ResponseEntity.ok("임시 비밀번호를 발송드렸습니다. 이메일을 확인해주세요");
	}
}
