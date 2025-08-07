package Team_Mute.back_end.domain.space_admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_admin_region")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRegion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "region_id")
	private Integer id;

	@Column(name = "region_name", nullable = false, length = 50)
	private String regionName;

	@Column(name = "reg_date")
	private LocalDateTime regDate;

	@Column(name = "upd_date")
	private LocalDateTime updDate;
}
