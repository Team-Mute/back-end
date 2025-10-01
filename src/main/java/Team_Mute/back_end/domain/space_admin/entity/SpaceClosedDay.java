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

import java.time.LocalDateTime;

/**
 * 공간 휴무일 Entity
 * - DB 테이블: tb_space_closedday
 * - 특정 공간의 휴무일(예약 불가 기간)을 관리
 * - Space 엔티티와 N:1 관계 (여러 휴무일이 하나의 공간에 속함)
 * - 공간 삭제 시, 관련 휴무일도 자동으로 삭제됨 (ON DELETE CASCADE)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_space_closedday")
public class SpaceClosedDay {
	/**
	 * 휴무일 고유 ID (PK, Auto Increment)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "closed_id")
	private Integer closedId;

	/**
	 * 연관된 공간 (FK: tb_spaces.space_id)
	 * - 여러 휴무일(N)이 하나의 공간(1)에 연결됨 (ManyToOne)
	 * - 지연 로딩(fetch = LAZY)으로 필요할 때만 로딩
	 * - 공간 삭제 시 관련 휴무일도 함께 삭제됨 (ON DELETE CASCADE)
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id", nullable = false, foreignKey = @ForeignKey(name = "fk_space_closedday"))
	@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
	private Space space;

	/**
	 * 휴무 시작일시 (NOT NULL)
	 */
	@Column(name = "closed_from", nullable = false)
	private LocalDateTime closedFrom;

	/**
	 * 휴무 종료일시 (NOT NULL)
	 */
	@Column(name = "closed_to", nullable = false)
	private LocalDateTime closedTo;
}
