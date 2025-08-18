package Team_Mute.back_end.domain.space_admin.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// 공간 저장 상태를 나타내는 Enum.
// DRAFT : 임시 저장 상태
// PUBLISHED : 저장 완료 상태

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_spaces")
public class Space {
	// 공간 고유 아이디
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "space_id")
	private Integer spaceId;

	// 지역 아이디(명동, 대구 등)
	@Column(name = "region_id")
	private Integer regionId;

	// 카테고리 아이디(미팅룸, 행사장 등)
	@Column(name = "category_id")
	private Integer categoryId;

	// 담당자 아이디
	@Column(name = "user_id")
	private Integer userId;

	// 공간 이름
	@Column(name = "space_name", unique = true, nullable = false, length = 100)
	private String spaceName;

	// 공간 수용 인원
	@Column(name = "space_capacity", nullable = false)
	private Integer spaceCapacity;

	// 공간 주소
	@Column(name = "location_id")
	private Integer locationId;

	// 공간 설명
	@Column(name = "space_description", nullable = false, columnDefinition = "TEXT")
	private String spaceDescription;

	// 대표 이미지 경로
	@Column(name = "space_image_url", nullable = false, columnDefinition = "TEXT")
	private String spaceImageUrl;

	// 공간 활성화 여부
	@Column(name = "space_is_available", nullable = false)
	private Boolean spaceIsAvailable;

	// 예약 방식
	@Column(name = "reservation_way", nullable = true, columnDefinition = "TEXT")
	private String reservationWay;

	// 이용 수칙
	@Column(name = "space_rules", nullable = true, columnDefinition = "TEXT")
	private String spaceRules;

	// 저장 상태[임시저장(DRAFT) or 저장(PUBLISHED)]
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "save_status")
	private SaveStatus saveStatus;

	// 등록일
	@Column(name = "reg_date", nullable = false)
	private LocalDateTime regDate;

	//수정일
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	// 공간 상세 이미지 Mapping
	@OneToMany(mappedBy = "space", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<SpaceImage> images = new ArrayList<>();

	// 공간 태그(화이트 보두, WIFI 등) Mapping
	@OneToMany(mappedBy = "space", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<SpaceTagMap> tagMaps = new ArrayList<>();

	// 공간 운영 시간 Mapping
	@OneToMany(mappedBy = "space", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<SpaceOperation> operations = new ArrayList<>();

	// 공간 휴무일 Mapping
	@OneToMany(mappedBy = "space", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<SpaceClosedDay> closedDays = new ArrayList<>();
}
