package Team_Mute.back_end.domain.space_admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_space_categories")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceCategory {

	@Id
	@Column(name = "category_id")
	private Integer id;

	@Column(name = "category_name", nullable = false, length = 50)
	private String categoryName;

	@Column(name = "reg_date")
	private LocalDateTime regDate;

	@Column(name = "upd_date")
	private LocalDateTime updDate;
}
