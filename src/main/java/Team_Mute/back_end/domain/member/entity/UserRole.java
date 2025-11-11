package Team_Mute.back_end.domain.member.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 역할 엔티티 클래스
 * 시스템 내 사용자 및 관리자의 역할(권한) 정보를 저장하는 엔티티
 * 역할에 따라 접근 가능한 기능과 권한이 구분됨
 * tb_user_roles 테이블과 매핑
 *
 * 역할 구분:
 * - 0: 마스터 관리자 (최고 권한, 모든 기능 접근 가능)
 * - 1: 2차 승인자 (예약 최종 승인 담당)
 * - 2: 1차 승인자 (지역별 예약 1차 검토 및 승인)
 * - 3: 일반 사용자 (공간 예약 및 조회)
 *
 * @author Team Mute
 * @since 1.0
 */
@Entity
@Table(name = "tb_user_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

	/**
	 * 역할 ID (Primary Key)
	 * - 수동 할당 방식 (자동 증가 아님)
	 * - 역할 고유 식별자
	 * - Integer 타입 사용
	 * - 0: 마스터 관리자
	 * - 1: 2차 승인자
	 * - 2: 1차 승인자
	 * - 3: 일반 사용자
	 */
	@Id
	@Column(name = "role_id")
	private Integer roleId;

	/**
	 * 역할명
	 * - 역할을 표시하는 이름
	 * - 최대 50자
	 * - NOT NULL 제약 조건
	 * - 예: "마스터 관리자", "1차 승인자", "2차 승인자", "일반 회원"
	 * - 화면에 표시할 역할 이름
	 */
	@Column(name = "role_name", length = 50, nullable = false)
	private String roleName;

	/**
	 * 등록 일시
	 * - 역할 정보 생성 시각
	 * - @CreationTimestamp로 자동 설정
	 * - NOT NULL 제약 조건
	 */
	@CreationTimestamp
	@Column(name = "reg_date", nullable = false)
	private LocalDateTime regDate;

	/**
	 * 수정 일시
	 * - 역할 정보 마지막 수정 시각
	 * - @UpdateTimestamp로 자동 갱신
	 * - NULL 허용
	 */
	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	/**
	 * 해당 역할을 가진 일반 사용자 목록 (일대다 관계)
	 * - User 엔티티와 OneToMany 관계
	 * - 한 역할에 여러 사용자 할당 가능
	 * - LAZY 로딩으로 성능 최적화
	 * - mappedBy로 User 엔티티의 userRole 필드와 연결
	 * - 양방향 관계 설정
	 */
	@OneToMany(mappedBy = "userRole", fetch = FetchType.LAZY)
	private List<User> users;

	/**
	 * 해당 역할을 가진 관리자 목록 (일대다 관계)
	 * - Admin 엔티티와 OneToMany 관계
	 * - 한 역할에 여러 관리자 할당 가능
	 * - LAZY 로딩으로 성능 최적화
	 * - mappedBy로 Admin 엔티티의 userRole 필드와 연결
	 * - 양방향 관계 설정
	 */
	@OneToMany(mappedBy = "userRole", fetch = FetchType.LAZY)
	private List<Admin> admin;

	/**
	 * 역할명을 받는 생성자
	 * - 역할 생성 시 편의성 제공
	 * - DataSeedRunner 등에서 초기 데이터 생성 시 사용
	 *
	 * @param roleName 생성할 역할명
	 */
	public UserRole(String roleName) {
		this.roleName = roleName;
	}
}
