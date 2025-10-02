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

/**
 * 공간 위치(Entity)
 * - DB 테이블: tb_locations
 * - 공간 주소를 관리
 * - 지역 정보(FK: tb_admin_region)와 연결
 */
@Entity
@Table(name = "tb_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceLocation {
	/**
	 * 위치 고유 ID (PK, Auto Increment)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "location_id")
	private Integer locationId;

	/**
	 * 연관된 지역(AdminRegion)
	 * - FK: tb_admin_region.region_id
	 * - 각 위치는 특정 지역(서울, 인천 등)에 속함
	 */
	@ManyToOne
	@JoinColumn(name = "region_id", referencedColumnName = "region_id")
	private AdminRegion adminRegion;

	/**
	 * 위치명 (예: 신한 스퀘어 브릿지 서울 등)
	 */
	@Column(name = "location_name", nullable = false, length = 150)
	private String locationName;

	/**
	 * 도로명 주소
	 * - 예: "서울특별시 중구 명동10길 52"
	 * - 도로명 주소 정보를 저장
	 * - 길이 제한: 200자
	 */
	@Column(name = "address_road", nullable = false, length = 200)
	private String addressRoad;

	/**
	 * 지번 주소
	 * - 예: "서울특별시 중구 충무로2가 65-4"
	 * - 지번 주소 정보를 저장
	 * - 길이 제한: 200자
	 */
	@Column(name = "address_jibun", nullable = false, length = 200)
	private String addressJibun;

	/**
	 * 우편번호
	 */
	@Column(name = "postal_code", length = 20)
	private String postalCode;

	/**
	 * 접근 정보
	 * - 예: "명동역 도보 2분"
	 * - 위치 접근 정보를 저장
	 * - 길이 제한: 200자 (부가 정보가 많아질 경우 확장 고려)
	 */
	@Column(name = "access_info", nullable = false, length = 200)
	private String accessInfo;

	/**
	 * 위치 활성화 여부 (기본값 true → 활성 상태)
	 */
	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true;

	/**
	 * 연관된 지역(AdminRegion) setter 메서드
	 */
	public void setAdminRegion(AdminRegion adminRegion) {
		if (adminRegion == null) {
			throw new IllegalArgumentException("공간은 반드시 지역과 연결되어야 합니다.");
		}
		this.adminRegion = adminRegion;
	}
}
