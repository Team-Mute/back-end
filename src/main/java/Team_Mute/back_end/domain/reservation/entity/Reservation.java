package Team_Mute.back_end.domain.reservation.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.global.util.StringListConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공간 예약 엔티티
 * 사용자가 공간을 예약한 정보를 관리하는 핵심 엔티티
 *
 * 테이블 구조:
 * - 테이블명: tb_reservations
 * - 기본 키: reservationId (자동 증가)
 * - 고유 식별자: orderId (사용자 친화적)
 *
 * 예약 워크플로우:
 * 1. 사용자 예약 생성 (상태: 승인 대기)
 * 2. 1차 승인자 검토 (상태: 1차 승인 또는 반려)
 * 3. 2차 승인자 검토 (상태: 최종 승인 또는 반려)
 * 4. 예약 사용 (상태: 이용 완료)
 * 5. 취소 가능 (상태: 예약 취소)
 *
 * 연관 관계:
 * - User (ManyToOne): 예약한 사용자
 * - Space (ManyToOne): 예약한 공간
 * - ReservationStatus (ManyToOne): 예약 상태
 * - PrevisitReservation (OneToOne): 사전답사 예약 (선택적)
 *
 * 첨부 파일:
 * - AWS S3 URL 목록
 *
 * @author Team Mute
 * @since 1.0
 */
@Entity
@Table(name = "tb_reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

	/**
	 * 예약 ID (기본 키)
	 * - 자동 증가 전략 (IDENTITY)
	 * - Long 타입
	 * - 내부적으로 사용되는 숫자 식별자
	 * - 데이터베이스에서 자동 생성
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reservation_id")
	private Long reservationId;

	/**
	 * 주문 ID (고유 식별자)
	 * - 사용자 친화적인 문자열 식별자
	 * - unique = true: 중복 불가
	 * - nullable = false: 필수 값
	 * - 사용자에게 표시되는 예약 번호
	 */
	@Column(name = "order_id", unique = true, nullable = false)
	private String orderId;

	/**
	 * 예약한 공간 (ManyToOne)
	 * - Space 엔티티와 N:1 관계
	 * - 외래 키: space_id
	 * - nullable = false: 필수 관계
	 * - FetchType.LAZY: 지연 로딩 (필요 시 조회)
	 *
	 * 관계 설명:
	 * - 하나의 공간은 여러 예약을 가질 수 있음
	 * - 하나의 예약은 하나의 공간만 가짐
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "space_id", nullable = false)
	private Space space;

	/**
	 * 예약한 사용자 (ManyToOne)
	 * - User 엔티티와 N:1 관계
	 * - 외래 키: user_id
	 * - nullable = false: 필수 관계
	 * - FetchType.LAZY: 지연 로딩
	 *
	 * 관계 설명:
	 * - 한 사용자는 여러 예약을 생성할 수 있음
	 * - 하나의 예약은 한 사용자에게 속함
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * 예약 상태 (ManyToOne)
	 * - ReservationStatus 엔티티와 N:1 관계
	 * - 외래 키: reservation_status_id
	 * - nullable = false: 필수 관계
	 * - FetchType.LAZY: 지연 로딩
	 *
	 * 상태 종류:
	 * - 1: 1차 승인 대기
	 * - 2: 2차 승인 대기
	 * - 3: 최종 승인 (예약 완료)
	 * - 4: 반려
	 * - 5: 이용 완료
	 * - 6: 예약 취소
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_status_id", nullable = false)
	private ReservationStatus reservationStatus;

	/**
	 * 사전답사 예약 (OneToOne)
	 * - PrevisitReservation 엔티티와 1:1 양방향 관계
	 * - mappedBy: 연관 관계의 주인은 PrevisitReservation
	 * - FetchType.LAZY: 지연 로딩
	 * - CascadeType.REMOVE: 예약 삭제 시 사전답사도 함께 삭제
	 * - null 허용: 모든 예약이 사전답사를 갖는 것은 아님
	 *
	 * 양방향 관계:
	 * - Reservation.previsitReservation (역방향)
	 * - PrevisitReservation.reservation (소유 측)
	 */
	@OneToOne(mappedBy = "reservation", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
	private PrevisitReservation previsitReservation;

	/**
	 * 예약 인원
	 * - 참석 예정 인원 수
	 * - 최소 1명 이상 (DTO 검증)
	 * - 공간의 최대 수용 인원 이하여야 함 (비즈니스 로직 검증)
	 */
	@Column(name = "reservation_headcount", nullable = false)
	private Integer reservationHeadcount;

	/**
	 * 예약 시작 시간
	 * - 예약 시작 일시
	 * - reservationTo보다 이전이어야 함
	 * - 기존 예약과 겹치지 않아야 함 (비즈니스 로직 검증)
	 */
	@Column(name = "reservation_from", nullable = false)
	private LocalDateTime reservationFrom;

	/**
	 * 예약 종료 시간
	 * - 예약 종료 일시
	 * - reservationFrom보다 이후여야 함
	 * - 공간의 운영 시간 내여야 함 (비즈니스 로직 검증)
	 */
	@Column(name = "reservation_to", nullable = false)
	private LocalDateTime reservationTo;

	/**
	 * 예약 목적
	 * - 예약 사유 또는 행사 내용
	 * - 관리자 승인 시 참고 자료
	 */
	@Column(name = "reservation_purpose", nullable = false, columnDefinition = "TEXT")
	private String reservationPurpose;

	/**
	 * 첨부 파일 URL 리스트
	 * - 예약 관련 문서 파일 (계획서, 제안서 등)
	 * - List<String>을 JSON 문자열로 변환하여 TEXT 컬럼에 저장
	 * - StringListConverter 사용 (AttributeConverter)
	 * - AWS S3에 업로드된 파일의 URL 목록
	 * - @Builder.Default: 빈 리스트로 초기화
	 *
	 * AttributeConverter:
	 * - DB 저장 시: List<String> → JSON String
	 * - 조회 시: JSON String → List<String>
	 */
	@Convert(converter = StringListConverter.class)
	@Column(name = "reservation_attachment", columnDefinition = "TEXT")
	@Builder.Default
	private List<String> reservationAttachment = new ArrayList<>();

	/**
	 * 등록 일시
	 * - 예약이 생성된 일시
	 */
	@CreationTimestamp
	@Column(name = "reg_date", updatable = false, nullable = false)
	private LocalDateTime regDate;

	/**
	 * 수정 일시
	 * - 예약이 마지막으로 수정된 일시
	 */
	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	// ==================== 비즈니스 로직 메서드 ====================

	/**
	 * 예약 정보 수정
	 * - 예약의 주요 정보를 일괄 업데이트하는 메서드
	 * - 트랜잭션 내에서 호출되어야 함
	 * - 변경 감지(Dirty Checking)로 자동 업데이트
	 *
	 * 수정 가능 항목:
	 * - 공간
	 * - 예약 상태
	 * - 인원
	 * - 시작/종료 시간
	 * - 목적
	 * - 첨부 파일
	 *
	 * 사용 예:
	 * reservation.updateDetails(
	 *   newSpace, newStatus, 30,
	 *   newFrom, newTo,
	 *   "수정된 목적", newAttachments
	 * );
	 *
	 * @param space 공간 엔티티
	 * @param status 예약 상태 엔티티
	 * @param headcount 예약 인원
	 * @param from 예약 시작 시간
	 * @param to 예약 종료 시간
	 * @param purpose 예약 목적
	 * @param attachment 첨부 파일 URL 리스트
	 */
	public void updateDetails(Space space, ReservationStatus status, Integer headcount, LocalDateTime from,
		LocalDateTime to, String purpose, List<String> attachment) {
		this.space = space;
		this.reservationStatus = status;
		this.reservationHeadcount = headcount;
		this.reservationFrom = from;
		this.reservationTo = to;
		this.reservationPurpose = purpose;
		this.reservationAttachment = attachment;
	}

	// ==================== Getter/Setter 메서드 ====================

	/**
	 * 예약 ID 조회
	 *
	 * @return 예약 ID (기본 키)
	 */
	public Long getReservationId() {
		return reservationId;
	}

	/**
	 * 예약한 공간 조회
	 * - getSpaceId() 이름이지만 Space 엔티티 반환
	 * - 레거시 코드 호환성 유지
	 *
	 * @return Space 엔티티
	 */
	public Space getSpaceId() {
		return space;
	}

	/**
	 * 예약한 공간 설정
	 *
	 * @param space Space 엔티티
	 */
	public void setSpaceId(Space space) {
		this.space = space;
	}

	/**
	 * 예약한 사용자 조회
	 * - getUserId() 이름이지만 User 엔티티 반환
	 * - 레거시 코드 호환성 유지
	 *
	 * @return User 엔티티
	 */
	public User getUserId() {
		return user;
	}

	/**
	 * 예약한 사용자 설정
	 *
	 * @param user User 엔티티
	 */
	public void setUserId(User user) {
		this.user = user;
	}

	/**
	 * 예약 상태 조회
	 * - getReservationStatusId() 이름이지만 ReservationStatus 엔티티 반환
	 * - 레거시 코드 호환성 유지
	 *
	 * @return ReservationStatus 엔티티
	 */
	public ReservationStatus getReservationStatusId() {
		return reservationStatus;
	}

	/**
	 * 예약 상태 설정
	 *
	 * @param reservationStatusId ReservationStatus 엔티티
	 */
	public void setReservationStatusId(ReservationStatus reservationStatusId) {
		this.reservationStatus = reservationStatusId;
	}

	/**
	 * 예약 인원 조회
	 *
	 * @return 예약 인원
	 */
	public Integer getReservationHeadcount() {
		return reservationHeadcount;
	}

	/**
	 * 예약 인원 설정
	 *
	 * @param reservationHeadcount 예약 인원
	 */
	public void setReservationHeadcount(Integer reservationHeadcount) {
		this.reservationHeadcount = reservationHeadcount;
	}

	/**
	 * 예약 시작 시간 조회
	 *
	 * @return 예약 시작 일시
	 */
	public LocalDateTime getReservationFrom() {
		return reservationFrom;
	}

	/**
	 * 예약 시작 시간 설정
	 *
	 * @param reservationFrom 예약 시작 일시
	 */
	public void setReservationFrom(LocalDateTime reservationFrom) {
		this.reservationFrom = reservationFrom;
	}

	/**
	 * 예약 종료 시간 조회
	 *
	 * @return 예약 종료 일시
	 */
	public LocalDateTime getReservationTo() {
		return reservationTo;
	}

	/**
	 * 예약 종료 시간 설정
	 *
	 * @param reservationTo 예약 종료 일시
	 */
	public void setReservationTo(LocalDateTime reservationTo) {
		this.reservationTo = reservationTo;
	}

	/**
	 * 예약 목적 조회
	 *
	 * @return 예약 목적
	 */
	public String getReservationPurpose() {
		return reservationPurpose;
	}

	/**
	 * 예약 목적 설정
	 *
	 * @param reservationPurpose 예약 목적
	 */
	public void setReservationPurpose(String reservationPurpose) {
		this.reservationPurpose = reservationPurpose;
	}

	/**
	 * 첨부 파일 URL 리스트 조회
	 *
	 * @return 첨부 파일 URL 리스트
	 */
	public List<String> getReservationAttachment() {
		return reservationAttachment;
	}

	/**
	 * 첨부 파일 URL 리스트 설정
	 *
	 * @param reservationAttachment 첨부 파일 URL 리스트
	 */
	public void setReservationAttachment(List<String> reservationAttachment) {
		this.reservationAttachment = reservationAttachment;
	}

	/**
	 * 주문 ID 조회
	 *
	 * @return 주문 ID (고유 식별자)
	 */
	public String getOrderId() {
		return orderId;
	}

	/**
	 * 주문 ID 설정
	 *
	 * @param orderId 주문 ID
	 */
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	/**
	 * 등록 일시 조회
	 *
	 * @return 등록 일시
	 */
	public LocalDateTime getRegDate() {
		return regDate;
	}

	/**
	 * 등록 일시 설정
	 * - 일반적으로 사용되지 않음 (@CreationTimestamp가 자동 설정)
	 *
	 * @param regDate 등록 일시
	 */
	public void setRegDate(LocalDateTime regDate) {
		this.regDate = regDate;
	}

	/**
	 * 수정 일시 조회
	 *
	 * @return 수정 일시
	 */
	public LocalDateTime getUpdDate() {
		return updDate;
	}

	/**
	 * 수정 일시 설정
	 * - 일반적으로 사용되지 않음 (@UpdateTimestamp가 자동 설정)
	 *
	 * @param updDate 수정 일시
	 */
	public void setUpdDate(LocalDateTime updDate) {
		this.updDate = updDate;
	}
}
