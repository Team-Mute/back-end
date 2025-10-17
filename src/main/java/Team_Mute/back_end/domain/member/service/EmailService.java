package Team_Mute.back_end.domain.member.service;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.global.constants.ReservationStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	private final JavaMailSender mailSender;
	@Value("${invite.base-url}")
	private String inviteBaseURL;

	public void sendTemporaryPassword(String toEmail, String temporaryPassword) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(toEmail);
			message.setSubject("[신한금융희망재단] 임시 비밀번호 안내");
			message.setText("안녕하세요, 신한금융희망재단 입니다.\n\n"
				+ "요청하신 임시 비밀번호는 다음과 같습니다:\n\n"
				+ "임시 비밀번호: " + temporaryPassword + "\n\n"
				+ "로그인 후 반드시 비밀번호를 변경해 주시기 바랍니다.\n"
				+ "감사합니다.");

			mailSender.send(message);
			log.info("임시 비밀번호 이메일 발송 성공: {}", toEmail);
		} catch (MailException e) {
			log.error("임시 비밀번호 이메일 발송 실패: {}", toEmail, e);
			throw new IllegalStateException("메일 발송에 실패했습니다.");
		}
	}

	public void sendAdminWelcomeEmail(String toEmail, String temporaryPassword) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(toEmail);
			message.setSubject("[신한금융희망재단] 관리자 계정 생성 완료 안내");
			message.setText("안녕하세요, 신한금융희망재단 입니다.\n\n"
				+ "귀하의 관리자 계정이 생성되었습니다.\n\n"
				+ "임시 비밀번호: " + temporaryPassword + "\n\n"
				+ "로그인 후 반드시 비밀번호를 변경해 주시기 바랍니다.\n"
				+ "감사합니다.");

			mailSender.send(message);
			log.info("관리자 계정 생성 안내 이메일 발송 성공: {}", toEmail);
		} catch (MailException e) {
			log.error("관리자 계정 생성 안내 이메일 발송 실패: {}", toEmail, e);
			throw new IllegalStateException("계정 생성 안내 메일 발송에 실패했습니다.");
		}
	}

	public void sendMailForReservationAdmin(Reservation reservation, Integer statusId, String rejectMsg) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		String OrderId = reservation.getOrderId();
		String userName = reservation.getUser().getUserName();
		String email = reservation.getUser().getUserEmail();
		String spaceName = reservation.getSpace().getSpaceName();
		String reservationFrom = reservation.getReservationFrom().format(formatter);
		String reservationTo = reservation.getReservationTo().format(formatter);

		// 초대장 링크
		String invitationURL = UriComponentsBuilder
			.fromHttpUrl(inviteBaseURL)
			.path("/{reservationId}")
			.buildAndExpand(reservation.getReservationId())
			.toUriString();

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);

		if (statusId.equals(ReservationStatusEnum.FINAL_APPROVAL.getId())) {
			message.setSubject("[신한금융희망재단] 최종 승인 완료 안내");
			message.setText("안녕하세요, 신한금융희망재단 입니다.\n\n"
				+ "신청하신 공간 예약이 완료되었습니다.\n\n"
				+ "예약 번호: " + OrderId + "\n"
				+ "예약자명: " + userName + "\n"
				+ "공간: " + spaceName + "\n"
				+ "예약 일시: " + reservationFrom + " ~ " + reservationTo + "\n\n"
				+ "초대장 URL\n" + invitationURL + "\n\n"
				+ "※ 예약 내역은 [마이페이지 > 공간 예약 내역 > 예약완료]에서 확인하실 수 있습니다.\n"
				+ "※ 고객센터: 070-5038-6828 (평일 09:00~18:00)");
		} else if (statusId.equals(ReservationStatusEnum.REJECTED_STATUS.getId())) {
			message.setSubject("[신한금융희망재단] 공간 예약 반려 안내");
			message.setText(
				"안녕하세요, 신한금융희망재단 입니다.\n\n"
					+ "신청하신 공간 예약이 반려되었습니다.\n\n" +
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
			mailSender.send(message);
		} catch (MailException e) {
			log.error("예약 상태 안내 이메일 발송 실패: {}", email, e);
			throw new IllegalStateException("예약 상태 안내 메일 발송에 실패했습니다.");
		}
	}
}
