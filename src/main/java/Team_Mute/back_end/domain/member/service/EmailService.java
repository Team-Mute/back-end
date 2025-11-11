package Team_Mute.back_end.domain.member.service;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.global.constants.ReservationStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이메일 발송 서비스 클래스
 * JavaMailSender를 사용하여 사용자 및 관리자에게 각종 알림 이메일 발송
 * SMTP 프로토콜을 통한 이메일 전송 기능 제공
 *
 * 주요 기능:
 * - 임시 비밀번호 이메일 발송 (비밀번호 초기화 시)
 * - 관리자 계정 생성 환영 이메일 발송
 * - 예약 상태 변경 알림 이메일 발송 (승인/반려)
 *
 * 이메일 종류:
 * 1. 임시 비밀번호 안내: 비밀번호를 잊어버린 사용자/관리자에게 발송
 * 2. 관리자 환영 메일: 신규 관리자 계정 생성 시 임시 비밀번호 포함하여 발송
 * 3. 예약 승인 완료: 최종 승인 시 초대장 URL 포함하여 발송
 * 4. 예약 반려 안내: 반려 사유와 함께 발송
 *
 * 보안 고려사항:
 * - SMTP 인증 정보는 application.properties에서 관리
 * - 이메일 발송 실패 시 로그 기록 및 예외 발생
 *
 * @author Team Mute
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	/**
	 * JavaMailSender 인스턴스
	 * - Spring Boot의 spring-boot-starter-mail 라이브러리에서 제공
	 * - SMTP 서버를 통한 이메일 발송 기능 제공
	 * - application.properties에서 SMTP 설정 자동 주입
	 *   (spring.mail.host, spring.mail.port, spring.mail.username, spring.mail.password 등)
	 * - SimpleMailMessage 또는 MimeMessage를 사용하여 이메일 전송
	 */
	private final JavaMailSender mailSender;

	/**
	 * 초대장 기본 URL
	 * - application.properties의 invite.base-url 값 주입
	 * - 예약 승인 시 초대장 페이지 URL 생성에 사용
	 */
	@Value("${invite.base-url}")
	private String inviteBaseURL;

	/**
	 * 임시 비밀번호 이메일 발송
	 * - 비밀번호 초기화 요청 시 호출
	 * - 사용자 또는 관리자가 비밀번호를 잊어버렸을 때 사용
	 * - AdminService.resetPassword() 또는 UserService.resetPassword()에서 호출
	 *
	 * 처리 흐름:
	 * 1. SimpleMailMessage 객체 생성
	 * 2. 수신자 이메일, 제목, 본문 설정
	 * 3. JavaMailSender를 통해 이메일 발송
	 * 4. 발송 성공 시 로그 기록
	 * 5. 발송 실패 시 예외 발생
	 *
	 * 이메일 내용:
	 * - 제목: [신한금융희망재단] 임시 비밀번호 안내
	 * - 본문: 임시 비밀번호와 변경 안내 메시지
	 *
	 * 보안 고려사항:
	 * - 임시 비밀번호는 generateRandomPassword()로 생성된 10자리 무작위 문자열
	 * - 로그인 후 반드시 비밀번호 변경 유도
	 * - 발송 실패 시 명확한 에러 메시지 제공
	 *
	 * @param toEmail 수신자 이메일 주소
	 * @param temporaryPassword 생성된 임시 비밀번호 (10자리)
	 * @throws IllegalStateException 이메일 발송 실패 시
	 */
	public void sendTemporaryPassword(String toEmail, String temporaryPassword) {
		try {
			// 1. SimpleMailMessage 객체 생성
			SimpleMailMessage message = new SimpleMailMessage();

			// 2. 수신자 설정
			message.setTo(toEmail);

			// 3. 이메일 제목 설정
			message.setSubject("[신한금융희망재단] 임시 비밀번호 안내");

			// 4. 이메일 본문 설정
			message.setText("안녕하세요, 신한금융희망재단 입니다.\n\n"
				+ "요청하신 임시 비밀번호는 다음과 같습니다:\n\n"
				+ "임시 비밀번호: " + temporaryPassword + "\n\n"
				+ "로그인 후 반드시 비밀번호를 변경해 주시기 바랍니다.\n"
				+ "감사합니다.");

			// 5. 이메일 발송
			mailSender.send(message);

			// 6. 발송 성공 로그
			log.info("임시 비밀번호 이메일 발송 성공: {}", toEmail);
		} catch (MailException e) {
			// 7. 발송 실패 로그 및 예외 발생
			log.error("임시 비밀번호 이메일 발송 실패: {}", toEmail, e);
			throw new IllegalStateException("메일 발송에 실패했습니다.");
		}
	}

	/**
	 * 관리자 환영 이메일 발송
	 * - 마스터 관리자가 신규 관리자 계정 생성 시 호출
	 * - AdminService.adminSignUp()에서 호출
	 * - 신규 관리자에게 임시 비밀번호와 함께 환영 메시지 전송
	 *
	 * 처리 흐름:
	 * 1. SimpleMailMessage 객체 생성
	 * 2. 수신자 이메일, 제목, 본문 설정
	 * 3. JavaMailSender를 통해 이메일 발송
	 * 4. 발송 성공 시 로그 기록
	 * 5. 발송 실패 시 예외 발생
	 *
	 * 이메일 내용:
	 * - 제목: [신한금융희망재단] 관리자 계정 생성 완료 안내
	 * - 본문: 계정 생성 안내와 임시 비밀번호, 변경 안내 메시지
	 *
	 * 사용 시나리오:
	 * - 마스터 관리자가 1차 승인자 또는 2차 승인자 계정 생성
	 * - 신규 관리자는 이메일로 받은 임시 비밀번호로 첫 로그인
	 * - 로그인 후 비밀번호 변경 필수
	 *
	 * @param toEmail 신규 관리자의 이메일 주소
	 * @param temporaryPassword 생성된 임시 비밀번호 (10자리)
	 * @throws IllegalStateException 이메일 발송 실패 시
	 */
	public void sendAdminWelcomeEmail(String toEmail, String temporaryPassword) {
		try {
			// 1. SimpleMailMessage 객체 생성
			SimpleMailMessage message = new SimpleMailMessage();

			// 2. 수신자 설정
			message.setTo(toEmail);

			// 3. 이메일 제목 설정
			message.setSubject("[신한금융희망재단] 관리자 계정 생성 완료 안내");

			// 4. 이메일 본문 설정
			message.setText("안녕하세요, 신한금융희망재단 입니다.\n\n"
				+ "귀하의 관리자 계정이 생성되었습니다.\n\n"
				+ "임시 비밀번호: " + temporaryPassword + "\n\n"
				+ "로그인 후 반드시 비밀번호를 변경해 주시기 바랍니다.\n"
				+ "감사합니다.");

			// 5. 이메일 발송
			mailSender.send(message);

			// 6. 발송 성공 로그
			log.info("관리자 계정 생성 안내 이메일 발송 성공: {}", toEmail);
		} catch (MailException e) {
			// 7. 발송 실패 로그 및 예외 발생
			log.error("관리자 계정 생성 안내 이메일 발송 실패: {}", toEmail, e);
			throw new IllegalStateException("계정 생성 안내 메일 발송에 실패했습니다.");
		}
	}

	/**
	 * 예약 상태 변경 알림 이메일 발송
	 * - 관리자가 예약을 승인하거나 반려할 때 호출
	 * - ReservationService에서 예약 상태 변경 후 호출
	 * - 사용자의 이메일 수신 동의 여부(agreeEmail)에 따라 발송 결정
	 *
	 * 처리 흐름:
	 * 1. 예약 정보에서 필요한 데이터 추출 (예약자명, 공간명, 예약 일시 등)
	 * 2. 초대장 URL 생성 (승인 시에만)
	 * 3. 상태에 따라 이메일 제목 및 본문 설정
	 *    - 최종 승인: 초대장 URL 포함
	 *    - 반려: 반려 사유 포함
	 * 4. JavaMailSender를 통해 이메일 발송
	 * 5. 발송 실패 시 예외 발생
	 *
	 * 이메일 종류:
	 * 1. 최종 승인 완료 (statusId = FINAL_APPROVAL)
	 *    - 제목: [신한금융희망재단] 최종 승인 완료 안내
	 *    - 본문: 예약 정보 + 초대장 URL + 마이페이지 안내
	 *
	 * 2. 반려 안내 (statusId = REJECTED_STATUS)
	 *    - 제목: [신한금융희망재단] 공간 예약 반려 안내
	 *    - 본문: 예약 정보 + 반려 사유 + 마이페이지 안내
	 *
	 * 초대장 URL 생성:
	 * - UriComponentsBuilder를 사용하여 동적 URL 생성
	 * - 형식: {inviteBaseURL}/{reservationId}
	 * - 예: https://mute.shinhancard.com/invitation/123
	 *
	 * @param reservation 예약 엔티티 (사용자, 공간, 예약 일시 정보 포함)
	 * @param statusId 변경된 예약 상태 ID
	 *                 - ReservationStatusEnum.FINAL_APPROVAL.getId(): 최종 승인
	 *                 - ReservationStatusEnum.REJECTED_STATUS.getId(): 반려
	 * @param rejectMsg 반려 사유 (반려 시에만 사용, 승인 시 null)
	 * @throws IllegalStateException 이메일 발송 실패 시
	 */
	public void sendMailForReservationAdmin(Reservation reservation, Integer statusId, String rejectMsg) {
		// 1. 날짜 포맷터 생성 (yyyy-MM-dd HH:mm 형식)
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

		// 2. 예약 정보 추출
		String OrderId = reservation.getOrderId();                        // 예약 번호 (고유 식별자)
		String userName = reservation.getUser().getUserName();            // 예약자 이름
		String email = reservation.getUser().getUserEmail();              // 예약자 이메일 주소
		String spaceName = reservation.getSpace().getSpaceName();         // 공간명
		String reservationFrom = reservation.getReservationFrom().format(formatter);  // 예약 시작 일시
		String reservationTo = reservation.getReservationTo().format(formatter);      // 예약 종료 일시

		// 3. 초대장 URL 생성
		// UriComponentsBuilder를 사용하여 reservationId를 Path Variable로 추가
		// 예: https://mute.shinhancard.com/invitation/123
		String invitationURL = UriComponentsBuilder
			.fromHttpUrl(inviteBaseURL)                        // 기본 URL
			.path("/{reservationId}")                          // Path Variable 템플릿
			.buildAndExpand(reservation.getReservationId())    // reservationId 값 삽입
			.toUriString();                                    // 최종 URL 문자열 생성

		// 4. SimpleMailMessage 객체 생성
		SimpleMailMessage message = new SimpleMailMessage();

		// 5. 수신자 설정
		message.setTo(email);

		// 6. 상태에 따라 이메일 제목 및 본문 설정
		if (statusId.equals(ReservationStatusEnum.FINAL_APPROVAL.getId())) {
			// 6-1. 최종 승인 완료 이메일
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
			// 6-2. 반려 안내 이메일
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

		// 7. 이메일 발송
		try {
			mailSender.send(message);
		} catch (MailException e) {
			// 8. 발송 실패 로그 및 예외 발생
			log.error("예약 상태 안내 이메일 발송 실패: {}", email, e);
			throw new IllegalStateException("예약 상태 안내 메일 발송에 실패했습니다.");
		}
	}
}
