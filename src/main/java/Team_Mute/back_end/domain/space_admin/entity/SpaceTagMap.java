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
import org.hibernate.annotations.CreationTimestamp;

/**
 * 공간-태그 매핑 Entity
 * - 공간(Space)과 태그(SpaceTag)의 다대다 관계를 풀어내는 연결 테이블
 * - 하나의 공간이 여러 태그를 가질 수 있고,
 * 하나의 태그가 여러 공간에 적용될 수 있음
 * - tb_space_tag_map 테이블 매핑
 */
@Entity
@Table(name = "tb_space_tag_map")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceTagMap {
	/**
	 * 매핑 고유 ID (PK, Auto Increment)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "map_id")
	private Integer id;

	/**
	 * 공간 ID (FK → tb_spaces.space_id)
	 * - 다대일 관계
	 * - 공간 삭제 시 연결된 매핑도 함께 삭제 (OnDelete CASCADE)
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id", nullable = false, foreignKey = @ForeignKey(name = "fk_space_tag_map_space"))
	@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
	private Space space;

	/**
	 * 태그 ID (FK → tb_space_tags.tag_id)
	 * - 다대일 관계
	 * - 특정 태그가 여러 공간에 연결 가능
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tag_id")
	private SpaceTag tag;

	/**
	 * 매핑 등록일
	 * - 언제 태그가 공간에 부여되었는지 기록
	 * - Entity 최초 생성 시 자동 입력
	 *
	 */
	@CreationTimestamp
	@Column(name = "reg_date")
	private LocalDateTime regDate;
}
