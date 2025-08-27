package Team_Mute.back_end.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.member.entity.UserCompany;
import Team_Mute.back_end.domain.member.entity.UserRole;
import Team_Mute.back_end.domain.member.repository.UserCompanyRepository;
import Team_Mute.back_end.domain.member.repository.UserRepository;
import Team_Mute.back_end.domain.member.repository.UserRoleRepository;
import Team_Mute.back_end.domain.reservation.dto.request.ReservationRequestDto;
import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.reservation.repository.ReservationRepository;
import Team_Mute.back_end.domain.reservation.repository.ReservationStatusRepository;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_admin.repository.SpaceRepository;
import Team_Mute.back_end.domain.space_admin.util.S3Deleter;
import Team_Mute.back_end.domain.space_admin.util.S3Uploader;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationServiceTest {

	@Autowired
	private SpaceRepository spaceRepository;

	@Autowired
	private ReservationStatusRepository reservationStatusRepository;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private UserRepository userRepository; // 테스트 데이터 준비용

	@MockitoBean
	private S3Uploader s3Uploader;

	@MockitoBean
	private S3Deleter s3Deleter;

	private User testUser;
	private Space testSpace;
	private Reservation testReservation;

	private final UserRoleRepository userRoleRepository;
	private final UserCompanyRepository userCompanyRepository;

	ReservationServiceTest(UserRoleRepository userRoleRepository, UserCompanyRepository userCompanyRepository) {
		this.userRoleRepository = userRoleRepository;
		this.userCompanyRepository = userCompanyRepository;
	}

	@BeforeEach
	void setUp() {
		UserRole testRole = UserRole.builder()
			.roleName("ROLE_USER")
			.build();
		userRoleRepository.save(testRole); // userRoleRepository 필요

		UserCompany testCompany = UserCompany.builder()
			.companyName("Test Company")
			.build();
		userCompanyRepository.save(testCompany); // userCompanyRepository 필요

		// 2. 테스트용 User 객체 생성 및 저장
		testUser = User.builder()
			.userEmail("testuser@" + System.currentTimeMillis() + ".com") // unique 제약조건 충족
			.userName("Test User")
			.userPhone("010-1234-5678")
			.userPwd("password123")
			.agreeEmail(true)
			.agreeSms(false)
			.agreeLocation(false)
			.userRole(testRole)       // 위에서 저장한 Role 객체
			.userCompany(testCompany) // 위에서 저장한 Company 객체
			.tokenVer(1)
			.build();
		userRepository.save(testUser);

		// 3. 테스트용 Space 객체 생성 및 저장
		testSpace = Space.builder()
			.regionId(1)
			.categoryId(1)
			.userId(testUser.getUserId())
			.spaceName("Test Space - " + System.currentTimeMillis()) // unique 제약조건 충족
			.spaceCapacity(20)
			.locationId(1)
			.spaceDescription("This is a test space for JUnit.")
			.spaceImageUrl("http://s3.com/default-space-image.jpg")
			.spaceIsAvailable(true)
			.reservationWay("ONLINE")
			.spaceRules("No smoking.")
			.regDate(LocalDateTime.now()) // @CreationTimestamp가 없으므로 수동 설정
			.build();
		spaceRepository.save(testSpace);

		// 4. 테스트용 ReservationStatus 객체 생성 및 저장
		ReservationStatus status = ReservationStatus.builder()
			.reservationStatusName("PENDING") // '승인대기', '확정' 등 상태명
			.build();
		reservationStatusRepository.save(status);

		// 5. 테스트용 Reservation 객체 생성 및 저장
		// 수정 및 삭제 테스트에서 사용할 예약 객체를 미리 생성합니다.
		testReservation = Reservation.builder()
			.orderId("Test-Order-" + System.currentTimeMillis())
			.space(testSpace)          // 위에서 저장한 Space 객체
			.user(testUser)            // 위에서 저장한 User 객체
			.reservationStatus(status) // 위에서 저장한 Status 객체
			.reservationHeadcount(5)
			.reservationFrom(LocalDateTime.now().plusDays(10))
			.reservationTo(LocalDateTime.now().plusDays(10).plusHours(2))
			.reservationPurpose("Initial Test Reservation")
			.reservationAttachment(List.of("http://s3.com/old-file.txt"))
			.build();

		reservationRepository.save(testReservation);
	}

	@Test
	@DisplayName("파일 첨부하여 예약 생성 성공")
	@WithMockUser(username = "1", authorities = {"ROLE_USER"})
		// role_id=3 가정
	void createReservation_withFiles_success() throws Exception {
		// given
		ReservationRequestDto requestDto = new ReservationRequestDto();
		requestDto.setSpaceId(testSpace.getSpaceId());
		requestDto.setReservationHeadcount(10);
		requestDto.setReservationFrom(LocalDateTime.now().plusDays(1));
		requestDto.setReservationTo(LocalDateTime.now().plusDays(1).plusHours(2));
		requestDto.setReservationPurpose("Create Test");

		MockMultipartFile file1 = new MockMultipartFile("files", "test1.txt", "text/plain", "test file 1" .getBytes());
		MockMultipartFile file2 = new MockMultipartFile("files", "test2.png", "image/png", "test image 2" .getBytes());

		String requestDtoJson = objectMapper.writeValueAsString(requestDto);
		MockMultipartFile jsonPart = new MockMultipartFile("requestDto", "", "application/json",
			requestDtoJson.getBytes(StandardCharsets.UTF_8));

		List<String> mockUrls = List.of("http://s3.com/mock-url-1", "http://s3.com/mock-url-2");
		given(s3Uploader.uploadAll(any(), anyString())).willReturn(mockUrls);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/reservations")
				.file(file1)
				.file(file2)
				.file(jsonPart))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.reservationAttachment").isArray())
			.andExpect(jsonPath("$.reservationAttachment[0]").value(mockUrls.get(0)))
			.andExpect(jsonPath("$.reservationAttachment[1]").value(mockUrls.get(1)));

		verify(s3Uploader, times(1)).uploadAll(any(), startsWith("attachment/"));
	}

	@Test
	@DisplayName("예약 수정 시 기존 파일 삭제 및 새 파일 업로드 성공")
	@WithMockUser(username = "1", authorities = {"ROLE_USER"})
	void updateReservation_withFiles_success() throws Exception {
		// given
		// 1. 기존 파일이 있는 예약 데이터 생성
		ReservationRequestDto requestDto = new ReservationRequestDto();
		requestDto.setSpaceId(testSpace.getSpaceId()); // spaceId 설정
		requestDto.setReservationHeadcount(20); // 수정할 인원
		requestDto.setReservationFrom(LocalDateTime.now().plusDays(5));
		requestDto.setReservationTo(LocalDateTime.now().plusDays(5).plusHours(3));
		requestDto.setReservationPurpose("Updated Purpose");
		// ... requestDto 업데이트 내용 설정

		MockMultipartFile newFile = new MockMultipartFile("files", "new.pdf", "application/pdf",
			"new file" .getBytes());
		String requestDtoJson = objectMapper.writeValueAsString(requestDto);
		MockMultipartFile jsonPart = new MockMultipartFile("requestDto", "", "application/json",
			requestDtoJson.getBytes(StandardCharsets.UTF_8));

		List<String> newUrls = List.of("http://s3.com/new-url.pdf");
		given(s3Uploader.uploadAll(any(), anyString())).willReturn(newUrls);
		willDoNothing().given(s3Deleter).deleteByUrl(anyString());

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/reservations/{id}", testReservation.getReservationId())
				.file(newFile)
				.file(jsonPart)
				.with(req -> {
					req.setMethod("PUT");
					return req;
				}))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reservationAttachment[0]").value(newUrls.get(0)));

		// verify
		verify(s3Deleter, times(1)).deleteByUrl(eq("http://s3.com/old-file.txt"));
		verify(s3Uploader, times(1)).uploadAll(any(), eq("attachment/" + testReservation.getReservationId()));

		Reservation updated = reservationRepository.findById(testReservation.getReservationId()).get();
		assertThat(updated.getReservationAttachment()).isEqualTo(newUrls);
	}

	@Test
	@DisplayName("예약 삭제 시 S3 파일 함께 삭제 성공")
	@WithMockUser(username = "1", authorities = {"ROLE_USER"})
	void deleteReservation_withFiles_success() throws Exception {
		List<String> urlsToDelete = testReservation.getReservationAttachment();

		willDoNothing().given(s3Deleter).deleteByUrl(anyString());
		ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/reservations/{id}", testReservation.getReservationId()))
			.andExpect(status().isNoContent());

		// verify
		// 첨부파일이 1개이므로 times(1)로 수정
		verify(s3Deleter, times(urlsToDelete.size())).deleteByUrl(urlCaptor.capture());
		List<String> capturedUrls = urlCaptor.getAllValues();
		assertThat(capturedUrls).containsExactlyInAnyOrderElementsOf(urlsToDelete);
	}
}
