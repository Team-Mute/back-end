package Team_Mute.back_end.domain.member.service;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	private final JavaMailSender mailSender;

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

}
