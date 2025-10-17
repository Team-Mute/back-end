package Team_Mute.back_end.domain.space_admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 공간 태그 Entity
 * - 특정 공간의 속성/편의시설 등을 나타내는 태그 정의
 * - 예: "WIFI", "화이트보드" 등
 * - 개별 공간과의 연결은 tb_space_tag_map 테이블을 통해 관리
 */
@Entity
@Table(name = "tb_space_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceTag {

	/**
	 * 태그 고유 ID (PK, Auto Increment)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "tag_id")
	private Integer tagId;

	/**
	 * 태그 이름
	 * - 최대 50자 제한
	 * - 예: "WIFI", "화이트보드"
	 */
	@Column(name = "tag_name", unique = true, nullable = false, length = 50)
	private String tagName;

	/**
	 * 등록일 (Entity 최초 생성 시 자동 입력)
	 */
	@CreationTimestamp
	@Column(name = "reg_date")
	private LocalDateTime regDate;

	/**
	 * 수정일 (Entity 변경 시 자동 갱신)
	 */
	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;
}
