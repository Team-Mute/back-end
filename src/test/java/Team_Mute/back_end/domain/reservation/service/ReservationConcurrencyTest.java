package Team_Mute.back_end.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.entity.UserRole;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.reservation.dto.request.ReservationRequestDto;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation.repository.ReservationStatusRepository;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
// 필요한 엔티티와 Repository import...

@SpringBootTest
class ReservationConcurrencyTest {

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private SpaceRepository spaceRepository;
	@Autowired
	private ReservationStatusRepository reservationStatusRepository;

	private Long userId;
	private Integer spaceId;

	@BeforeEach
	@Transactional
		// 테스트 데이터 준비는 별도 트랜잭션으로 처리
	void setUp() {
		// 테스트에 필요한 사용자, 공간, 예약 상태 데이터 미리 저장
		UserRole userRole = new UserRole(3);
		// userRoleRepository.save(userRole); // 필요시

		User testUser = new User();
		testUser.setUserRole(userRole);
		// ... user 정보 설정 ...
		userRepository.save(testUser);
		this.userId = testUser.getUserId();

		Space testSpace = new Space();
		// ... space 정보 설정 ...
		spaceRepository.save(testSpace);
		this.spaceId = testSpace.getSpaceId();

		ReservationStatus testStatus = new ReservationStatus(1L, "1차 승인 대기");
		reservationStatusRepository.save(testStatus);
	}

	@Test
	@DisplayName("동시에 100개의 동일한 예약 생성 요청 시, 단 1개만 성공해야 한다")
	void createReservation_Concurrency_Test() throws InterruptedException {
		// given
		int threadCount = 100;
		// 멀티스레드 테스트를 위한 ExecutorService 생성
		ExecutorService executorService = Executors.newFixedThreadPool(32);
		// 모든 스레드가 끝날 때까지 대기하기 위한 CountDownLatch
		CountDownLatch latch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);

		ReservationRequestDto requestDto = new ReservationRequestDto();
		requestDto.setSpaceId(this.spaceId);
		requestDto.setReservationFrom(LocalDateTime.of(2025, 11, 1, 10, 0));
		requestDto.setReservationTo(LocalDateTime.of(2025, 11, 1, 12, 0));
		requestDto.setReservationHeadcount(10);
		requestDto.setReservationPurpose("Concurrent Test");

		// when
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					// 각 스레드가 예약 생성 서비스 호출
					reservationService.createReservation(this.userId.toString(), requestDto);
					successCount.incrementAndGet(); // 성공 시 카운트 증가
				} catch (Exception e) {
					// DataIntegrityViolationException 등 예외 발생 시 실패 카운트 증가
					failureCount.incrementAndGet();
				} finally {
					latch.countDown(); // 스레드 작업 완료
				}
			});
		}

		latch.await(); // 모든 스레드가 작업을 마칠 때까지 대기
		executorService.shutdown();

		// then
		// 100개의 요청 중, 단 1개만 성공하고 99개는 실패해야 한다.
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failureCount.get()).isEqualTo(99);
	}
}
