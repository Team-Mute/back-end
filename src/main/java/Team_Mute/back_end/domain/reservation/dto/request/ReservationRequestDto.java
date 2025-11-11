package Team_Mute.back_end.domain.reservation.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 예약 생성 요청 DTO
 * 새로운 공간 예약을 생성하기 위한 요청 데이터
 * multipart/form-data 형식으로 전송 (파일 첨부 지원)
 *
 * 사용 목적:
 * - 공간 예약 신청 (사용자 → 관리자 승인 대기)
 * - 첨부 파일 업로드 (계획서, 제안서 등)
 * - 사전답사 예약 포함 (선택적)
 *
 * API 엔드포인트:
 * - POST /api/reservations
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
@Setter
public class ReservationRequestDto {

	/**
	 * 공간 ID (필수)
	 * - 예약할 공간의 고유 식별자
	 * - Space 엔티티의 spaceId 참조
	 */
	@NotNull(message = "공간 ID는 필수입니다.")
	private Integer spaceId;

	/**
	 * 예약 인원 (필수)
	 * - 예약 시 참석 예정 인원 수
	 * - 공간의 최대 수용 인원 이하여야 함 (서비스 레이어 검증)
	 * - 1명 이상 필수
	 */
	@NotNull(message = "예약 인원은 필수입니다.")
	@Min(value = 1, message = "예약 인원은 1명 이상이어야 합니다.")
	private Integer reservationHeadcount;

	/**
	 * 예약 시작 시간 (필수)
	 * - 예약 시작 일시
	 * - 현재 시간 이후여야 함 (@Future)
	 * - reservationTo보다 이전이어야 함 (서비스 레이어 검증)
	 * - 기존 예약과 겹치지 않아야 함 (서비스 레이어 검증)
	 */
	@NotNull(message = "예약 시작 시간은 필수입니다.")
	@Future(message = "현재 시간 이후여야 합니다.")
	private LocalDateTime reservationFrom;

	/**
	 * 예약 종료 시간 (필수)
	 * - 예약 종료 일시
	 * - reservationFrom보다 이후여야 함 (서비스 레이어 검증)
	 * - 공간의 운영 시간 내여야 함 (서비스 레이어 검증)
	 */
	@NotNull(message = "예약 종료 시간은 필수입니다.")
	private LocalDateTime reservationTo;

	/**
	 * 예약 목적 (필수)
	 * - 예약 사유 또는 행사 내용
	 * - 관리자 승인 시 참고 자료
	 */
	@NotBlank(message = "예약 목적은 필수입니다.")
	private String reservationPurpose;

	/**
	 * 예약 첨부 파일 리스트 (선택적)
	 * - 예약 관련 문서 파일 (계획서, 제안서, 참고 자료 등)
	 * - AWS S3에 업로드 후 URL 저장
	 */
	private List<MultipartFile> reservationAttachments;

	/**
	 * 기존 첨부 파일 URL 리스트 (선택적)
	 * - 예약 수정 시 기존에 업로드된 파일의 URL
	 * - 삭제하지 않을 기존 파일 유지용
	 * - S3 URL 문자열 리스트
	 */
	private List<String> existingAttachments;

	/**
	 * 사전답사 정보 (선택적)
	 * - 본 예약 전 공간을 미리 방문하는 사전답사 예약
	 * - PrevisitInfoDto 중첩 클래스로 정의
	 */
	@Valid
	private PrevisitInfoDto previsitInfo;

	/**
	 * 기존 첨부 파일 URL 리스트 조회
	 * - Getter 메서드
	 *
	 * @return 기존 첨부 파일 URL 리스트
	 */
	public List<String> getExistingAttachments() {
		return existingAttachments;
	}

	/**
	 * 기존 첨부 파일 URL 리스트 설정
	 * - Setter 메서드
	 *
	 * @param existingAttachments 기존 첨부 파일 URL 리스트
	 */
	public void setExistingAttachments(List<String> existingAttachments) {
		this.existingAttachments = existingAttachments;
	}

	/**
	 * 사전답사 정보 조회
	 * - Getter 메서드
	 *
	 * @return 사전답사 정보 DTO
	 */
	public PrevisitInfoDto getPrevisitInfo() {
		return previsitInfo;
	}

	/**
	 * 사전답사 정보 설정
	 * - Setter 메서드
	 *
	 * @param previsitInfo 사전답사 정보 DTO
	 */
	public void setPrevisitInfo(PrevisitInfoDto previsitInfo) {
		this.previsitInfo = previsitInfo;
	}

	/**
	 * 사전답사 정보 중첩 DTO
	 * - 본 예약 전 공간 방문 예약
	 * - 공간 상태 확인, 배치 계획 등의 목적
	 */
	@Getter
	@Setter
	public static class PrevisitInfoDto {
		/**
		 * 사전답사 시작 시간 (필수)
		 * - 사전답사 시작 일시
		 * - 본 예약(reservationFrom) 이전이어야 함
		 */
		@NotNull(message = "사전답사 시작 시간은 필수입니다.")
		private LocalDateTime previsitFrom;

		/**
		 * 사전답사 종료 시간 (필수)
		 * - 사전답사 종료 일시
		 * - previsitFrom보다 이후여야 함
		 */
		@NotNull(message = "사전답사 종료 시간은 필수입니다.")
		private LocalDateTime previsitTo;
	}
}
