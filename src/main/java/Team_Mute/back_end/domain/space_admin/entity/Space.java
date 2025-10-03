package Team_Mute.back_end.domain.space_admin.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 공간 Entity
 * - DB 테이블: tb_spaces
 * - 하나의 공간(예: 미팅룸, 행사장 등)의 기본 정보를 저장
 * - 지역, 카테고리, 사용자와 연계되며, 이미지/태그/운영시간/휴무일 등과 1:N 관계를 가짐
 * - 공간 단위의 생성/수정/삭제에 따라 관련된 하위 엔티티들도 함께 관리됨(cascade + orphanRemoval)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_spaces")
public class Space {
	/**
	 * 공간 고유 ID (PK, Auto Increment)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "space_id")
	private Integer spaceId;

	/**
	 * 지역 ID (예: 서울, 대구 등) → FK (tb_regions.region_id)
	 */
	@Column(name = "region_id", nullable = false)
	private Integer regionId;

	/**
	 * 카테고리 ID
	 * - 숫자 코드 값 (예: 1=미팅룸, 2=행사장, ...)
	 * - FK (tb_space_categories.category_id)
	 */
	@Column(name = "category_id", nullable = false)
	private Integer categoryId;

	/**
	 * 담당자 ID → FK (tb_admins.user_id)
	 */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	/**
	 * 공간 이름 (고유값, NOT NULL, 최대 100자)
	 */
	@Column(name = "space_name", unique = true, nullable = false, length = 100)
	private String spaceName;

	/**
	 * 공간 수용 인원 (NOT NULL)
	 */
	@Column(name = "space_capacity", nullable = false)
	private Integer spaceCapacity;

	/**
	 * 공간 주소 ID → FK (tb_locations.location_id)
	 */
	@Column(name = "location_id", nullable = false)
	private Integer locationId;

	/**
	 * 공간 설명 (상세 정보, NOT NULL, TEXT 타입)
	 */
	@Column(name = "space_description", nullable = false, columnDefinition = "TEXT")
	private String spaceDescription;

	/**
	 * 대표 이미지 경로 (NOT NULL, TEXT 타입, S3 URL 등 저장)
	 */
	@Column(name = "space_image_url", nullable = false, columnDefinition = "TEXT")
	private String spaceImageUrl;

	/**
	 * 공간 활성화 여부 (예약 가능 여부 판단)
	 */
	@Column(name = "space_is_available", nullable = false)
	private Boolean spaceIsAvailable;

	/**
	 * 예약 방식 (예: 웹 신청 후 관리자 승인, TEXT 타입)
	 */
	@Column(name = "reservation_way", nullable = false, columnDefinition = "TEXT")
	private String reservationWay;

	/**
	 * 이용 수칙 (이용자에게 제공되는 규칙, TEXT 타입)
	 */
	@Column(name = "space_rules", nullable = false, columnDefinition = "TEXT")
	private String spaceRules;

	/**
	 * 등록일 (공간 최초 등록 시각)
	 */
	@CreationTimestamp
	@Column(name = "reg_date", nullable = false)
	private LocalDateTime regDate;

	/**
	 * 수정일 (마지막 수정 시각, NULL 허용)
	 */
	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	/**
	 * 공간 상세 이미지 리스트
	 * - DB 테이블: tb_space_images
	 * - 1:N 관계 (SpaceImage)
	 * - 공간 삭제 시 관련 이미지도 함께 삭제(cascade = REMOVE)
	 * - orphanRemoval: 리스트에서 제거된 엔티티도 자동 삭제
	 */
	@OneToMany(mappedBy = "space", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<SpaceImage> images = new ArrayList<>();

	/**
	 * 공간 태그 매핑 리스트 (예: 화이트보드, WIFI 등)
	 * - DB 테이블: tb_space_tag_map
	 * - 1:N 관계 (SpaceTagMap)
	 * - 공간 삭제 시 태그 매핑도 함께 삭제
	 */
	@OneToMany(mappedBy = "space", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<SpaceTagMap> tagMaps = new ArrayList<>();

	/**
	 * 공간 운영 시간 리스트
	 * - DB 테이블: tb_space_operation
	 * - 1:N 관계 (SpaceOperation)
	 * - 특정 공간의 운영 가능한 시간대 정보 관리
	 */
	@OneToMany(mappedBy = "space", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<SpaceOperation> operations = new ArrayList<>();

	/**
	 * 공간 휴무일 리스트
	 * - DB 테이블: tb_space_colsedday
	 * - 1:N 관계 (SpaceClosedDay)
	 * - 특정 공간이 예약 불가한 날짜 관리
	 */
	@OneToMany(mappedBy = "space", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<SpaceClosedDay> closedDays = new ArrayList<>();
}
