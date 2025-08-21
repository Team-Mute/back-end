package Team_Mute.back_end.domain.space_admin.entity;

import Team_Mute.back_end.domain.member.entity.AdminRegion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceLocation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "location_id")
	private Integer locationId;

	@ManyToOne
	@JoinColumn(name = "region_id", referencedColumnName = "region_id")
	private AdminRegion adminRegion;


	@Column(name = "location_name", nullable = false, length = 150)
	private String locationName;

	@Column(name = "address_road", nullable = false, length = 200)
	private String addressRoad;

	@Column(name = "postal_code", length = 20)
	private String postalCode;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true;

	// setter 추가
	public void setAdminRegion(AdminRegion adminRegion) {
		this.adminRegion = adminRegion;
	}
}
