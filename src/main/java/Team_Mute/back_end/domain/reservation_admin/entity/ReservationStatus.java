package Team_Mute.back_end.domain.reservation_admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_reservation_status")
public class ReservationStatus {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reservation_status_id")
	private Integer reservationStatusId;

	@Column(name = "reservation_status_name", nullable = false, unique = true, length = 50)
	private String reservationStatusName;

	@Column(name = "description")
	private String description;

	public Integer getReservationStatusId() {
		return reservationStatusId;
	}

	public String getReservationStatusName() {
		return reservationStatusName;
	}

	public void setReservationStatusName(String v) {
		this.reservationStatusName = v;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String v) {
		this.description = v;
	}
}
