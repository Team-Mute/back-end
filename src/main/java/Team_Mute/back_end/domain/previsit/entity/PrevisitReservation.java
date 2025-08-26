package Team_Mute.back_end.domain.previsit.entity;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
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
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Table(name = "tb_previsit_reservations")
public class PrevisitReservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "previsit_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@Column(name = "reservation_status_id", nullable = false)
	private Long reservationStatusId;

	@Column(name = "previsit_from", nullable = false)
	private LocalDateTime previsitFrom;

	@Column(name = "previsit_to", nullable = false)
	private LocalDateTime previsitTo;

	@CreationTimestamp
	@Column(name = "reg_date", updatable = false)
	private LocalDateTime regDate;

	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	// getters/setters
	public Long getPrevisitId() {
		return id;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public void setReservation(Reservation r) {
		this.reservation = r;
	}

	public Long getReservationStatusId() {
		return reservationStatusId;
	}

	public void setReservationStatusId(Long v) {
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
