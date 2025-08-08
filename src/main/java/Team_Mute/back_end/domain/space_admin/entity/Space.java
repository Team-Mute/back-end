package Team_Mute.back_end.domain.space_admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_spaces")
public class Space {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "space_id")
	private Integer spaceId;

	@Column(name = "region_id")
	private Integer regionId;

	@Column(name = "category_id")
	private Integer categoryId;

	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "space_name", length = 100)
	private String spaceName;

	@Column(name = "space_capacity")
	private Integer spaceCapacity;

	@Column(name = "space_location", length = 256)
	private String spaceLocation;

	@Column(name = "space_description", columnDefinition = "TEXT")
	private String spaceDescription;

	@Column(name = "space_image_url", columnDefinition = "TEXT")
	private String spaceImageUrl;

	@Column(name = "space_is_available")
	private Boolean spaceIsAvailable;

	@Column(name = "reg_date", insertable = false, updatable = false)
	private java.sql.Timestamp regDate;

	@Column(name = "upd_date", insertable = true, updatable = true)
	private java.sql.Timestamp updDate;
}
