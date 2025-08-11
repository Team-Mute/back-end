package Team_Mute.back_end.domain.space_admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_space_tags")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceTag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "tag_id")
	private Integer id;

	@Column(name = "tag_name", nullable = false, length = 50)
	private String tagName;

	@Column(name = "reg_date")
	private LocalDateTime regDate;

	@Column(name = "upd_date")
	private LocalDateTime updDate;
}
