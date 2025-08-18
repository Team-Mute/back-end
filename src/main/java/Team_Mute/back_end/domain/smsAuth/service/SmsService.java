package Team_Mute.back_end.domain.smsAuth.service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;

import Team_Mute.back_end.domain.smsAuth.exception.InvalidVerificationException;
import Team_Mute.back_end.domain.smsAuth.exception.SmsSendingFailedException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

	private final StringRedisTemplate redisTemplate;

	@Value("${sms.api.key}")
	private String apiKey;
	@Value("${sms.api.secret}")
	private String apiSecret;
	@Value("${sms.sender-number}")
	private String senderNumber;

	private DefaultMessageService messageService;

	@PostConstruct
	private void init() {
		this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.coolsms.co.kr");
	}

	private String createRandomNumber() {
		Random rand = new Random();
		StringBuilder numStr = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			numStr.append(rand.nextInt(10));
		}
		return numStr.toString();
	}

	public void sendSms(String countryCode, String phoneNumber) {
		String verificationCode = createRandomNumber();
		String fullPhoneNumber = phoneNumber.replaceAll("-", "");

		Message message = new Message();
		message.setFrom(senderNumber);
		message.setTo(fullPhoneNumber);
		message.setText("[신한금융희망재단 인증] 인증번호는 [" + verificationCode + "] 입니다.");

		try {
			SingleMessageSentResponse response = this.messageService.sendOne(new SingleMessageSendingRequest(message));
			assert response != null;
			if (!"2000".equals(response.getStatusCode())) {
				System.out.println("SMS 발송 실패: " + response.getStatusMessage());
				throw new SmsSendingFailedException("SMS 발송에 실패했습니다.");
			}

			redisTemplate.opsForValue().set(fullPhoneNumber, verificationCode, 3, TimeUnit.MINUTES);

		} catch (Exception e) {
			System.err.println("SMS 발송 중 예상치 못한 오류 발생: " + e.getMessage());
			throw new SmsSendingFailedException("SMS 발송에 실패했습니다.");
		}
	}

	public void verifySms(String phoneNumber, String verificationCode) {
		String storedCode = redisTemplate.opsForValue().get(phoneNumber);
		if (storedCode == null || !storedCode.equals(verificationCode)) {
			throw new InvalidVerificationException("인증번호가 올바르지 않습니다.");
		}
		redisTemplate.delete(phoneNumber);
	}
}
