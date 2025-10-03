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
 * 공간 카테고리 Entity
 * - DB 테이블: tb_space_categories
 * - 공간의 유형(예: 미팅룸, 행사장, 다목적)을 관리
 * - Space 엔티티와 N:1 관계로 연결됨 (FK: category_id)
 * - 등록일/수정일은 Hibernate 어노테이션으로 자동 관리됨
 */
@Entity
@Table(name = "tb_space_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceCategory {
	/**
	 * 카테고리 고유 ID (PK, Auto Increment)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "category_id")
	private Integer categoryId;

	/**
	 * 카테고리명 (NOT NULL, 최대 50자)
	 */
	@Column(name = "category_name", nullable = false, length = 50)
	private String categoryName;

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
