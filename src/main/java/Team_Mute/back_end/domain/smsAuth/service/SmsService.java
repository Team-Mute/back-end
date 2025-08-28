package Team_Mute.back_end.domain.smsAuth.service;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.smsAuth.exception.InvalidVerificationException;
import Team_Mute.back_end.domain.smsAuth.exception.SmsSendingFailedException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;

import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

	private final StringRedisTemplate redisTemplate;
	private static final Long APPROVED_FINAL_ID = 3L; // 최종 승인
	private static final Long REJECTED_STATUS_ID = 4L; // 반려

	@Value("${sms.api.key}")
	private String apiKey;
	@Value("${sms.api.secret}")
	private String apiSecret;
	@Value("${sms.sender-number}")
	private String senderNumber;
	@Value("${invite.base-url}")
	private String inviteBaseURL;

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

	// 예약 관리 시 사용 -> 최종승인, 반려 시 사용자에게 문자 전송
	public void sendSmsForReservationAdmin(String countryCode, Reservation reservation, Long statusId, String rejectMsg) {
		String verificationCode = createRandomNumber();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		String OrderId = reservation.getOrderId();
		String userName = reservation.getUser().getUserName();
		String phoneNumber = reservation.getUser().getUserPhone();
		String spaceName = reservation.getSpace().getSpaceName();
		String reservationFrom = reservation.getReservationFrom().format(formatter);
		String reservationTo = reservation.getReservationTo().format(formatter);
		// 초대장 링크
		String invitationURL = UriComponentsBuilder
			.fromHttpUrl(inviteBaseURL)
			.path("/{reservationId}")
			.buildAndExpand(reservation.getReservationId())
			.toUriString();

		Message message = new Message();
		message.setFrom(senderNumber);
		message.setTo(phoneNumber);
		if (statusId.equals(APPROVED_FINAL_ID)) {
			message.setText(
				"[신한금융희망재단] 최종 승인 완료\n" +
					"예약 번호: " + OrderId + "\n" +
					"예약자명: " + userName + "\n" +
					"공간: " + spaceName + "\n" +
					"예약 일시: " + reservationFrom + " ~ " + reservationTo + "\n\n" +
					"초대장 URL\n" + invitationURL + "\n\n" +

					"※ 예약 내역은 [마이페이지 > 공간 예약 내역 > 예약완료]에서 확인하실 수 있습니다.\n" +
					"※ 고객센터: 070-5038-6828 (평일 09:00~18:00)"
			);
		} else if (statusId.equals(REJECTED_STATUS_ID)) {
			message.setText(
				"[신한금융희망재단] 공간 예약 반려\n" +
					"예약 번호: " + OrderId + "\n" +
					"예약자명: " + userName + "\n" +
					"공간: " + spaceName + "\n" +
					"예약 일시: " + reservationFrom + " ~ " + reservationTo + "\n" +
					"반려 사유: " + rejectMsg + "\n\n" +

					"※ 반려 내역은 [마이페이지 > 공간 예약 내역 > 취소]에서 확인하실 수 있습니다.\n" +
					"※ 고객센터: 070-5038-6828 (평일 09:00~18:00)"
			);
		}


		try {
			SingleMessageSentResponse response = this.messageService.sendOne(new SingleMessageSendingRequest(message));
			assert response != null;
			if (!"2000".equals(response.getStatusCode())) {
				System.out.println("SMS 발송 실패: " + response.getStatusMessage());
				throw new SmsSendingFailedException("SMS 발송에 실패했습니다.");
			}

			redisTemplate.opsForValue().set(phoneNumber, verificationCode, 3, TimeUnit.MINUTES);

		} catch (Exception e) {
			System.err.println("SMS 발송 중 예상치 못한 오류 발생: " + e.getMessage());
			throw new SmsSendingFailedException("SMS 발송에 실패했습니다.");
		}
	}
}
