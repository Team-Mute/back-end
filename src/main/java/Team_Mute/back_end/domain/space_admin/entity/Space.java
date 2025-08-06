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
	@Column(name = "space_id")
	private Integer id;

	//@ManyToOne(fetch = FetchType.LAZY)
	//@JoinColumn(name = "category_id")
	//private SpaceCategory category;
	@Column(name = "category_id")
	private Integer category_id;

	@Column(name = "region_id", nullable = false)
	private Integer regionId;

	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "space_name", nullable = false, length = 100)
	private String name;

	@Column(name = "space_capacity")
	private Integer capacity;

	@Column(name = "space_location", length = 256)
	private String location;

	@Column(name = "space_description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "space_image_url", columnDefinition = "TEXT")
	private String imageUrl;

	@Column(name = "space_is_available")
	private Boolean isAvailable;

	@Column(name = "reg_date")
	private LocalDateTime regDate;

	@Column(name = "upd_date")
	private LocalDateTime updDate;
}
