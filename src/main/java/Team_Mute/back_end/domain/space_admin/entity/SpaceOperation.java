package Team_Mute.back_end.domain.space_admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_space_operation")
public class SpaceOperation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "operation_id")
	private Integer operationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id", nullable = false, foreignKey = @ForeignKey(name = "fk_space_operation"))
	@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
	private Space space;

	@Column(name = "day", nullable = false)   // 1=월 ~ 7=일
	private Integer day;

	@Column(name = "operation_from", nullable = false)
	private LocalTime operationFrom;

	@Column(name = "operation_to", nullable = false)
	private LocalTime operationTo;

	@Column(name = "is_open", nullable = false)
	private Boolean isOpen;

}
