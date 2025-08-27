package Team_Mute.back_end.domain.reservation.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import Team_Mute.back_end.domain.member.entity.User;
import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reservation_id")
	private Long reservationId; // PK: Long 타입, 자동 증가

	@Column(name = "order_id", unique = true, nullable = false)
	private String orderId; // 기존 reservation_id 역할, 고유 식별자

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "space_id", nullable = false)
	private Space space;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_status_id", nullable = false)
	private ReservationStatus reservationStatus;

	@OneToMany(mappedBy = "reservation", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<PrevisitReservation> previsitReservations = new ArrayList<>();

	@Column(name = "reservation_headcount", nullable = false)
	private Integer reservationHeadcount;

	@Column(name = "reservation_from", nullable = false)
	private LocalDateTime reservationFrom;

	@Column(name = "reservation_to", nullable = false)
	private LocalDateTime reservationTo;

	@Column(name = "reservation_purpose", nullable = false, columnDefinition = "TEXT")
	private String reservationPurpose;

	@Convert(converter = StringListConverter.class)
	@Column(name = "reservation_attachment", columnDefinition = "TEXT")
	@Builder.Default
	private List<String> reservationAttachment = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "reg_date", updatable = false)
	private LocalDateTime regDate;

	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	// 수정 메서드 (내용 동일)
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

	// getters/setters
	public Long getReservationId() {
		return reservationId;
	}

	public Space getSpaceId() {
		return space;
	}

	public void setSpaceId(Space space) {
		this.space = space;
	}

	public User getUserId() {
		return user;
	}

	public void setUserId(User user) {
		this.user = user;
	}

	public ReservationStatus getReservationStatusId() {
		return reservationStatus;
	}

	public void setReservationStatusId(ReservationStatus reservationStatusId) {
		this.reservationStatus = reservationStatusId;
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

	public List<String> getReservationAttachment() {
		return reservationAttachment;
	}

	public void setReservationAttachment(List<String> reservationAttachment) {
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
