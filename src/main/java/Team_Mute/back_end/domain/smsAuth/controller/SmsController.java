package Team_Mute.back_end.domain.smsAuth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Team_Mute.back_end.domain.smsAuth.dto.request.SmsRequestDto;
import Team_Mute.back_end.domain.smsAuth.dto.request.VerificationRequestDto;
import Team_Mute.back_end.domain.smsAuth.dto.response.VerificationResponseDto;
import Team_Mute.back_end.domain.smsAuth.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sms")
public class SmsController {

	private final SmsService smsService;

	@PostMapping("/send")
	public ResponseEntity<String> sendSms(@Valid @RequestBody SmsRequestDto requestDto) {
		smsService.sendSms(requestDto.getCountryCode(), requestDto.getPhoneNumber());
		return ResponseEntity.ok("인증번호가 성공적으로 발송되었습니다.");
	}

	@PostMapping("/verify")
	public ResponseEntity<?> verifySms(@Valid @RequestBody VerificationRequestDto requestDto) {
		smsService.verifySms(requestDto.getPhoneNumber(), requestDto.getVerificationCode());

		return ResponseEntity.ok(new VerificationResponseDto(requestDto.getPhoneNumber()));
	}
}
