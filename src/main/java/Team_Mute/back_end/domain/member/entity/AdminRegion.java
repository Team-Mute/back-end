package Team_Mute.back_end.domain.member.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 관리자 담당 지역 엔티티 클래스
 * 관리자의 담당 지역 정보를 저장하는 엔티티
 * 1차 승인자가 관리하는 지역 범위를 정의
 * 예약 승인 시 담당 지역별로 관리자를 필터링하는 데 사용
 * tb_admin_region 테이블과 매핑
 *
 * @author Team Mute
 * @since 1.0
 */
@Entity
@Table(name = "tb_admin_region")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRegion {

	/**
	 * 지역 ID (Primary Key)
	 * - 자동 증가(IDENTITY) 전략으로 생성
	 * - 지역 고유 식별자
	 * - Integer 타입 사용
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "region_id")
	private Integer regionId;

	/**
	 * 지역명
	 * - 담당 지역 이름 (예: "서울", "경기", "부산" 등)
	 * - 최대 50자
	 * - NOT NULL 제약 조건
	 * - UNIQUE 제약 조건으로 중복 불가
	 * - 예약 승인 시 관리자 필터링에 사용
	 */
	@Column(name = "region_name", length = 50, nullable = false, unique = true)
	private String regionName;

	/**
	 * 등록 일시
	 * - 지역 정보 생성 시각
	 * - @CreationTimestamp로 자동 설정
	 * - NOT NULL 제약 조건
	 */
	@CreationTimestamp
	@Column(name = "reg_date", nullable = false)
	private LocalDateTime regDate;

	/**
	 * 수정 일시
	 * - 지역 정보 마지막 수정 시각
	 * - @UpdateTimestamp로 자동 갱신
	 * - NULL 허용
	 */
	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	/**
	 * 해당 지역을 담당하는 관리자 목록 (일대다 관계)
	 * - Admin 엔티티와 OneToMany 관계
	 * - 한 지역에 여러 관리자 배정 가능
	 * - LAZY 로딩으로 성능 최적화
	 * - mappedBy로 Admin 엔티티의 adminRegion 필드와 연결
	 * - 양방향 관계 설정
	 */
	@OneToMany(mappedBy = "adminRegion", fetch = FetchType.LAZY)
	private List<Admin> admin;

	/**
	 * 지역명을 받는 생성자
	 * - 지역 생성 시 편의성 제공
	 * - DataSeedRunner 등에서 초기 데이터 생성 시 사용
	 *
	 * @param regionName 생성할 지역명
	 */
	public AdminRegion(String regionName) {
		this.regionName = regionName;
	}
}
