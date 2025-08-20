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

@Entity
@Table(name = "tb_admin_region")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRegion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "region_id")
	private Integer regionId;

	@Column(name = "region_name", nullable = false, length = 50)
	private String regionName;

	@Column(name = "reg_date")
	private LocalDateTime regDate;

	@Column(name = "upd_date")
	private LocalDateTime updDate;
}
