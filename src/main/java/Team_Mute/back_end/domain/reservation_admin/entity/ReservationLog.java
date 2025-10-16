package Team_Mute.back_end.domain.reservation_admin.entity;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * [예약 반려 사유 로그] 엔티티
 * - 예약(Reservation)의 반려 사유를 기록하는 엔티티
 */
@Entity
@Table(name = "tb_reservation_logs")
@Getter
@Setter
public class ReservationLog {
	/**
	 * 로그 고유 ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 상태 변경이 발생한 예약 엔티티 (Reservation)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	/**
	 * 변경된 최종 상태 엔티티 (ReservationStatus)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "changed_status_id", nullable = false)
	private ReservationStatus changedStatus;

	/**
	 * 상태 변경 시 기록된 반려 사유
	 */
	@Column(name = "memo", columnDefinition = "TEXT", nullable = false)
	private String memo;

	/**
	 * 로그 기록 일시 (자동 생성)
	 */
	@CreationTimestamp
	@Column(name = "reg_date", updatable = false)
	private LocalDateTime regDate;
}
