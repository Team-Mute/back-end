package Team_Mute.back_end.domain.space_admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_spaces")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Space {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "space_id")
	private Integer id;

	//@ManyToOne(fetch = FetchType.LAZY)
	//@JoinColumn(name = "category_id")
	//private SpaceCategory category;
	@Column(name = "category_id")
	private Integer categoryId;

	@Column(name = "region_id", nullable = false)
	private Integer regionId;

	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "space_name", nullable = false, length = 100)
	private String spaceName;

	@Column(name = "space_capacity")
	private Integer spaceCapacity;

	@Column(name = "space_location", length = 256)
	private String spaceLocation;

	@Column(name = "space_description", columnDefinition = "TEXT")
	private String spaceDescription;

	@Column(name = "space_image_url", columnDefinition = "TEXT")
	private String imageUrl;

	@Column(name = "space_is_available")
	private Boolean spaceAvailable;

	@Column(name = "reg_date")
	private LocalDateTime regDate;

	@Column(name = "upd_date")
	private LocalDateTime updDate;
}
