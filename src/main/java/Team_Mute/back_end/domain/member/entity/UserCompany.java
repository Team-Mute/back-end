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
 * 사용자 소속 기업 엔티티 클래스
 * 사용자와 관리자의 소속 기업 정보를 저장하는 엔티티
 * 외부 기업 정보 조회 API를 통해 검색한 법인 등록 기업 정보
 * tb_user_company 테이블과 매핑
 *
 * @author Team Mute
 * @since 1.0
 */
@Entity
@Table(name = "tb_user_company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompany {

	/**
	 * 기업 ID (Primary Key)
	 * - 수동 할당 방식 (자동 증가 아님)
	 * - 기업 고유 식별자
	 * - Integer 타입 사용
	 * - 외부 API에서 받은 기업 코드 또는 자체 관리 ID
	 */
	@Id
	@Column(name = "company_id")
	private Integer companyId;

	/**
	 * 기업명
	 * - 법인에 등록된 정식 기업명
	 * - 최대 50자
	 * - NOT NULL 제약 조건
	 * - CorpInfoController를 통해 검색한 기업명 사용 권장
	 * - 회원가입 및 예약 시 표시
	 */
	@Column(name = "company_name", length = 50, nullable = false)
	private String companyName;

	/**
	 * 등록 일시
	 * - 기업 정보 생성 시각
	 * - @CreationTimestamp로 자동 설정
	 * - NOT NULL 제약 조건
	 */
	@CreationTimestamp
	@Column(name = "reg_date", nullable = false)
	private LocalDateTime regDate;

	/**
	 * 수정 일시
	 * - 기업 정보 마지막 수정 시각
	 * - @UpdateTimestamp로 자동 갱신
	 * - NULL 허용
	 */
	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	/**
	 * 해당 기업에 소속된 일반 사용자 목록 (일대다 관계)
	 * - User 엔티티와 OneToMany 관계
	 * - 한 기업에 여러 사용자 소속 가능
	 * - LAZY 로딩으로 성능 최적화
	 * - mappedBy로 User 엔티티의 userCompany 필드와 연결
	 * - 양방향 관계 설정
	 * - 기업별 사용자 통계 및 관리에 사용
	 */
	@OneToMany(mappedBy = "userCompany", fetch = FetchType.LAZY)
	private List<User> users;

	/**
	 * 해당 기업에 소속된 관리자 목록 (일대다 관계)
	 * - Admin 엔티티와 OneToMany 관계
	 * - 한 기업에 여러 관리자 소속 가능
	 * - LAZY 로딩으로 성능 최적화
	 * - mappedBy로 Admin 엔티티의 userCompany 필드와 연결
	 * - 양방향 관계 설정
	 * - NULL 허용 (기관 관리자는 소속 기업 없음)
	 */
	@OneToMany(mappedBy = "userCompany", fetch = FetchType.LAZY)
	private List<Admin> admin;
}
