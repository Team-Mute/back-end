package Team_Mute.back_end.domain.space_admin.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "tb_space_images")
public class SpaceImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "image_id")
	private Integer imageId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id")
	private Space space;                     // FK → tb_spaces.space_id

	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	@Column(name = "image_priority", nullable = false)
	private Integer imagePriority;

	@Column(name = "reg_date", insertable = false, updatable = false)
	private java.sql.Timestamp regDate;

	@Column(name = "upd_date", insertable = true, updatable = true)
	private java.sql.Timestamp updDate;
}
