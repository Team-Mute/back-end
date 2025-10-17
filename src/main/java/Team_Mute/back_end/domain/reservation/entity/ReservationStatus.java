package Team_Mute.back_end.domain.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_reservation_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationStatus {

	@Id
	@Column(name = "reservation_status_id")
	private Integer reservationStatusId;

	@Column(name = "reservation_status_name", nullable = false)
	private String reservationStatusName;
}
