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

/**
 * 공간 운영 시간 Entity
 * - DB 테이블: tb_space_operation
 * - 공간별 요일 단위 운영 시간(시작/종료)을 관리
 * - 예: 월요일 09:00~18:00, 토요일 10:00~16:00, 일요일 휴무 등
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_space_operation")
public class SpaceOperation {
	/**
	 * 운영 시간 고유 ID (PK, Auto Increment)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "operation_id")
	private Integer operationId;

	/**
	 * 공간 ID (FK → tb_spaces.space_id)
	 * - 다대일 관계 (여러 운영시간 → 하나의 공간)
	 * - 공간 삭제 시 해당 운영 시간도 함께 삭제 (OnDelete CASCADE)
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id", nullable = false, foreignKey = @ForeignKey(name = "fk_space_operation"))
	@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
	private Space space;

	/**
	 * 요일 (1=월요일 ~ 7=일요일)
	 * - DB 저장 시 정수값으로 관리
	 * - 프론트/비즈니스 로직에서 요일 문자열로 변환 가능
	 */
	@Column(name = "day", nullable = false)
	private Integer day;

	/**
	 * 운영 시작 시간 (예: 09:00)
	 */
	@Column(name = "operation_from", nullable = false)
	private LocalTime operationFrom;

	/**
	 * 운영 종료 시간 (예: 18:00)
	 */
	@Column(name = "operation_to", nullable = false)
	private LocalTime operationTo;

	/**
	 * 해당 요일 운영 여부
	 * - true = 영업일, false = 휴무일
	 */
	@Column(name = "is_open", nullable = false)
	private Boolean isOpen;
}
