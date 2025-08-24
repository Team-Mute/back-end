package Team_Mute.back_end.domain.reservation_admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_reservations")
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reservation_id")
	private Integer reservationId;

	@Column(name = "space_id", nullable = false)
	private Integer spaceId;          // tb_spaces FK 예정 (지금은 숫자만)

	@Column(name = "user_id", nullable = false)
	private Long userId;           // tb_users FK 예정 (지금은 숫자만)

	@Column(name = "reservation_status_id", nullable = false)
	private Integer reservationStatusId;  // 상태 ID (Status 테이블 참조용)

	@Column(name = "reservation_headcount", nullable = false)
	private Integer reservationHeadcount;

	@Column(name = "reservation_from", nullable = false)
	private LocalDateTime reservationFrom;

	@Column(name = "reservation_to", nullable = false)
	private LocalDateTime reservationTo;

	@Column(name = "reservation_purpose", length = 255)
	private String reservationPurpose;

	@Column(name = "reservation_attachment", length = 255)
	private String reservationAttachment;

	@Column(name = "order_id", length = 50)
	private String orderId;

	@Column(name = "reg_date")
	private LocalDateTime regDate = LocalDateTime.now();

	@Column(name = "upd_date")
	private LocalDateTime updDate = LocalDateTime.now();

	// getters/setters
	public Integer getReservationId() {
		return reservationId;
	}

	public Integer getSpaceId() {
		return spaceId;
	}

	public void setSpaceId(Integer spaceId) {
		this.spaceId = spaceId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Integer getReservationStatusId() {
		return reservationStatusId;
	}

	public void setReservationStatusId(Integer reservationStatusId) {
		this.reservationStatusId = reservationStatusId;
	}

	public Integer getReservationHeadcount() {
		return reservationHeadcount;
	}

	public void setReservationHeadcount(Integer reservationHeadcount) {
		this.reservationHeadcount = reservationHeadcount;
	}

	public LocalDateTime getReservationFrom() {
		return reservationFrom;
	}

	public void setReservationFrom(LocalDateTime reservationFrom) {
		this.reservationFrom = reservationFrom;
	}

	public LocalDateTime getReservationTo() {
		return reservationTo;
	}

	public void setReservationTo(LocalDateTime reservationTo) {
		this.reservationTo = reservationTo;
	}

	public String getReservationPurpose() {
		return reservationPurpose;
	}

	public void setReservationPurpose(String reservationPurpose) {
		this.reservationPurpose = reservationPurpose;
	}

	public String getReservationAttachment() {
		return reservationAttachment;
	}

	public void setReservationAttachment(String reservationAttachment) {
		this.reservationAttachment = reservationAttachment;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}


	public LocalDateTime getRegDate() {
		return regDate;
	}

	public void setRegDate(LocalDateTime regDate) {
		this.regDate = regDate;
	}

	public LocalDateTime getUpdDate() {
		return updDate;
	}

	public void setUpdDate(LocalDateTime updDate) {
		this.updDate = updDate;
	}
}
