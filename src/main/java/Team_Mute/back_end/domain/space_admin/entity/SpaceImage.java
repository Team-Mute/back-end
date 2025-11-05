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
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 공간 이미지 Entity
 * - DB 테이블: tb_space_images
 * - 특정 공간에 속하는 상세 이미지들을 관리
 * - 공간 삭제 시, 관련 이미지도 자동 삭제 (ON DELETE CASCADE)
 * - 이미지 순서(imagePriority) 기준으로 정렬/노출 가능
 */
@Getter
@Setter
@Entity
@Table(name = "tb_space_images")
public class SpaceImage {
	/**
	 * 이미지 고유 ID (PK, Auto Increment)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "image_id")
	private Integer imageId;

	/**
	 * 연관된 공간 (FK: tb_spaces.space_id)
	 * - 여러 이미지(N)이 하나의 공간(1)에 속함
	 * - 지연 로딩(fetch = LAZY) 사용 → 필요할 때만 공간 데이터 조회
	 * - 공간 삭제 시 해당 공간의 이미지들도 함께 삭제됨 (ON DELETE CASCADE)
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id", nullable = false, foreignKey = @ForeignKey(name = "fk_space_images_space"))
	@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
	private Space space;

	/**
	 * 이미지 URL (NOT NULL) → AWS S3 경로
	 */
	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	/**
	 * 이미지 노출 우선순위 (작을수록 먼저 노출됨)
	 */
	@Column(name = "image_priority", nullable = false)
	private Integer imagePriority;

	/**
	 * 등록일 (Entity 최초 생성 시 자동 입력)
	 */
	@CreationTimestamp
	@Column(name = "reg_date", insertable = false, updatable = false, nullable = false)
	private LocalDateTime regDate;

	/**
	 * 수정일 (Entity 변경 시 자동 갱신)
	 */
	@UpdateTimestamp
	@Column(name = "upd_date", insertable = true, updatable = true)
	private LocalDateTime updDate;
}
