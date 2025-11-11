package Team_Mute.back_end.domain.reservation.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 사전답사 예약 엔티티
 * 공간 예약(Reservation) 전에 미리 공간을 방문하는 사전답사 예약 정보
 *
 * 테이블 구조:
 * - 테이블명: tb_previsit_reservations
 * - 본 예약(Reservation)과 OneToOne 양방향 관계
 * - 모든 예약이 사전답사를 갖는 것은 아님 (선택적)
 *
 * 사용 목적:
 * - 행사 전 공간 상태 확인
 * - 배치 계획 수립
 * - 공간 사용 가능 여부 사전 점검
 *
 * 비즈니스 규칙:
 * - 사전답사 시간은 본 예약 시간 이전이어야 함
 * - 하나의 예약당 최대 하나의 사전답사만 가능
 * - 사전답사도 공간 예약 스케줄에 포함됨
 *
 * 연관 관계:
 * - Reservation (OneToOne, 양방향): 본 예약
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "tb_previsit_reservations")
public class PrevisitReservation {

	/**
	 * 사전답사 ID (기본 키)
	 * - 자동 증가 전략 (IDENTITY)
	 * - 데이터베이스에서 자동 생성
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "previsit_id")
	private Long id;

	/**
	 * 연관된 예약 (OneToOne)
	 * - 본 예약(Reservation) 엔티티와 1:1 관계
	 * - 외래 키: reservation_id
	 * - nullable = false: 필수 관계
	 * - 본 예약이 삭제되면 사전답사도 함께 삭제 (CascadeType.REMOVE)
	 *
	 * 양방향 관계:
	 * - Reservation.previsitReservation (mappedBy)
	 * - PrevisitReservation.reservation (소유 측)
	 */
	@OneToOne
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	/**
	 * 사전답사 시작 시간
	 * - 사전답사 시작 일시
	 * - LocalDateTime 타입 (년-월-일 시:분:초)
	 * - nullable = false: 필수 값
	 * - 본 예약(reservationFrom) 이전이어야 함 (비즈니스 로직 검증)
	 */
	@Column(name = "previsit_from", nullable = false)
	private LocalDateTime previsitFrom;

	/**
	 * 사전답사 종료 시간
	 * - 사전답사 종료 일시
	 * - LocalDateTime 타입
	 * - nullable = false: 필수 값
	 * - previsitFrom보다 이후여야 함 (비즈니스 로직 검증)
	 */
	@Column(name = "previsit_to", nullable = false)
	private LocalDateTime previsitTo;

	/**
	 * 등록 일시
	 * - 사전답사 예약이 생성된 일시
	 * - @CreationTimestamp: 엔티티 생성 시 자동 설정
	 * - updatable = false: 수정 불가
	 * - nullable = false: 필수 값
	 */
	@CreationTimestamp
	@Column(name = "reg_date", updatable = false, nullable = false)
	private LocalDateTime regDate;

	/**
	 * 수정 일시
	 * - 사전답사 예약이 마지막으로 수정된 일시
	 * - @UpdateTimestamp: 엔티티 수정 시 자동 갱신
	 * - null 허용 (수정되지 않은 경우)
	 */
	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	/**
	 * 사전답사 ID 조회
	 * - id 필드의 Getter
	 * - 외부에서는 previsitId로 접근
	 *
	 * @return 사전답사 ID
	 */
	public Long getPrevisitId() {
		return id;
	}

	/**
	 * 연관된 예약 조회
	 * - reservation 필드의 Getter
	 *
	 * @return Reservation 엔티티
	 */
	public Reservation getReservation() {
		return reservation;
	}

	/**
	 * 연관된 예약 설정
	 * - reservation 필드의 Setter
	 * - 양방향 관계 설정 시 사용
	 *
	 * @param r Reservation 엔티티
	 */
	public void setReservation(Reservation r) {
		this.reservation = r;
	}

	/**
	 * 사전답사 시작 시간 조회
	 *
	 * @return 사전답사 시작 일시
	 */
	public LocalDateTime getPrevisitFrom() {
		return previsitFrom;
	}

	/**
	 * 사전답사 시작 시간 설정
	 *
	 * @param v 사전답사 시작 일시
	 */
	public void setPrevisitFrom(LocalDateTime v) {
		this.previsitFrom = v;
	}

	/**
	 * 사전답사 종료 시간 조회
	 *
	 * @return 사전답사 종료 일시
	 */
	public LocalDateTime getPrevisitTo() {
		return previsitTo;
	}

	/**
	 * 사전답사 종료 시간 설정
	 *
	 * @param v 사전답사 종료 일시
	 */
	public void setPrevisitTo(LocalDateTime v) {
		this.previsitTo = v;
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
	 * @param v 등록 일시
	 */
	public void setRegDate(LocalDateTime v) {
		this.regDate = v;
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
	 * @param v 수정 일시
	 */
	public void setUpdDate(LocalDateTime v) {
		this.updDate = v;
	}
}
