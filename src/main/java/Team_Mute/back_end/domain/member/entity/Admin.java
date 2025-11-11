package Team_Mute.back_end.domain.member.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 관리자 엔티티 클래스
 * 공간 예약 시스템을 관리하는 관리자 계정 정보를 저장하는 엔티티
 * 마스터 관리자, 1차 승인자, 2차 승인자로 역할이 구분됨
 * JWT 인증 시스템과 Token Versioning을 사용하여 보안 강화
 * tb_admins 테이블과 매핑
 *
 * @author Team Mute
 * @since 1.0
 */
@Entity
@Table(name = "tb_admins")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {

	/**
	 * 관리자 ID (Primary Key)
	 * - 자동 증가(IDENTITY) 전략으로 생성
	 * - 관리자 고유 식별자
	 * - Long 타입으로 대용량 데이터 지원
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "admin_id")
	private Long adminId;

	/**
	 * 관리자 이메일 주소
	 * - 로그인 ID로 사용
	 * - 최대 100자
	 * - NOT NULL 제약 조건
	 * - UNIQUE 제약 조건으로 중복 불가
	 * - 관리자 계정 식별자
	 */
	@Column(name = "admin_email", length = 100, nullable = false, unique = true)
	private String adminEmail;

	/**
	 * 관리자 이름
	 * - 실명
	 * - 최대 50자
	 * - NOT NULL 제약 조건
	 * - 시스템 내 표시 이름
	 */
	@Column(name = "admin_name", length = 50, nullable = false)
	private String adminName;

	/**
	 * 관리자 전화번호
	 * - 연락 가능한 전화번호
	 * - 최대 20자
	 * - NOT NULL 제약 조건
	 * - 긴급 상황 시 연락용
	 */
	@Column(name = "admin_phone", length = 20, nullable = false)
	private String adminPhone;

	/**
	 * 관리자 비밀번호 (암호화됨)
	 * - BCrypt 알고리즘으로 암호화하여 저장
	 * - 최대 255자 (BCrypt 해시 길이 고려)
	 * - NOT NULL 제약 조건
	 * - 평문 저장 금지
	 * - PasswordService를 통해 암호화 처리
	 */
	@Column(name = "admin_pwd", length = 255, nullable = false)
	private String adminPwd;

	/**
	 * 등록 일시
	 * - 관리자 계정 생성 시각
	 * - @CreationTimestamp로 자동 설정
	 * - NOT NULL 제약 조건
	 * - 변경 불가 (insertable=true, updatable=false)
	 */
	@CreationTimestamp
	@Column(name = "reg_date", nullable = false)
	private LocalDateTime regDate;

	/**
	 * 수정 일시
	 * - 관리자 정보 마지막 수정 시각
	 * - @UpdateTimestamp로 자동 갱신
	 * - NULL 허용 (최초 생성 시)
	 * - 수정 시마다 자동 업데이트
	 */
	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	/**
	 * 관리자 역할 (다대일 관계)
	 * - UserRole 엔티티와 ManyToOne 관계
	 * - 0: 마스터 관리자 (최고 권한)
	 * - 1: 2차 승인자 (최종 승인)
	 * - 2: 1차 승인자 (지역별 예약 1차 검토)
	 * - LAZY 로딩으로 성능 최적화
	 * - NOT NULL 제약 조건
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id", nullable = false)
	private UserRole userRole;

	/**
	 * 소속 기업 (다대일 관계)
	 * - UserCompany 엔티티와 ManyToOne 관계
	 * - 관리자의 소속 기업 정보
	 * - LAZY 로딩으로 성능 최적화
	 * - NULL 허용 (기관 관리자인 경우)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id")
	private UserCompany userCompany;

	/**
	 * 담당 지역 (다대일 관계)
	 * - AdminRegion 엔티티와 ManyToOne 관계
	 * - 1차 또는 2차 승인자의 담당 지역
	 * - LAZY 로딩으로 성능 최적화
	 * - NULL 허용 (마스터 관리자는 담당 지역 없음)
	 * - 예약 승인 시 담당 지역 필터링에 사용
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "region_id")
	private AdminRegion adminRegion;

	/**
	 * 토큰 버전
	 * - JWT Refresh Token 무효화를 위한 버전 관리
	 * - 비밀번호 변경, 보안 사고 발생 시 증가
	 * - 기본값 1
	 * - NOT NULL 제약 조건
	 * - 토큰 재발급 시 현재 버전과 비교하여 유효성 검증
	 * - 버전이 다르면 기존 토큰 모두 무효화
	 */
	@Column(name = "token_ver", nullable = false)
	private Integer tokenVer = 1;
}
