package Team_Mute.back_end.domain.reservation_admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_previsit_reservations")
public class PrevisitReservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "previsit_id")
	private Integer previsitId;

	/**
	 * 예약 FK
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	/**
	 * 상태 ID (공용 상태 테이블 사용)
	 */
	@Column(name = "reservation_status_id", nullable = false)
	private Integer reservationStatusId;

	@Column(name = "previsit_from", nullable = false)
	private LocalDateTime previsitFrom;

	@Column(name = "previsit_to", nullable = false)
	private LocalDateTime previsitTo;

	@Column(name = "reg_date")
	private LocalDateTime regDate = LocalDateTime.now();

	@Column(name = "upd_date")
	private LocalDateTime updDate = LocalDateTime.now();

	// getters/setters
	public Integer getPrevisitId() {
		return previsitId;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public void setReservation(Reservation r) {
		this.reservation = r;
	}

	public Integer getReservationStatusId() {
		return reservationStatusId;
	}

	public void setReservationStatusId(Integer v) {
		this.reservationStatusId = v;
	}

	public LocalDateTime getPrevisitFrom() {
		return previsitFrom;
	}

	public void setPrevisitFrom(LocalDateTime v) {
		this.previsitFrom = v;
	}

	public LocalDateTime getPrevisitTo() {
		return previsitTo;
	}

	public void setPrevisitTo(LocalDateTime v) {
		this.previsitTo = v;
	}

	public LocalDateTime getRegDate() {
		return regDate;
	}

	public void setRegDate(LocalDateTime v) {
		this.regDate = v;
	}

	public LocalDateTime getUpdDate() {
		return updDate;
	}

	public void setUpdDate(LocalDateTime v) {
		this.updDate = v;
	}
}
