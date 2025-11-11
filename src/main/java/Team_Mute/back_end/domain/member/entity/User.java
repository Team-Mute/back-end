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
 * 사용자 엔티티 클래스
 * 공간 예약 서비스를 이용하는 일반 사용자 정보를 저장하는 엔티티
 * JWT 인증 시스템과 Token Versioning을 사용하여 보안 강화
 * 소속 기업 정보를 필수로 입력받아 기업 단위 관리 가능
 * tb_users 테이블과 매핑
 *
 * @author Team Mute
 * @since 1.0
 */
@Entity
@Table(name = "tb_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	/**
	 * 사용자 ID (Primary Key)
	 * - 자동 증가(IDENTITY) 전략으로 생성
	 * - 사용자 고유 식별자
	 * - Long 타입으로 대용량 데이터 지원
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	/**
	 * 사용자 이메일 주소
	 * - 로그인 ID로 사용
	 * - 최대 100자
	 * - NOT NULL 제약 조건
	 * - UNIQUE 제약 조건으로 중복 불가
	 * - 사용자 계정 식별자
	 * - 회원가입 시 중복 체크 필수
	 */
	@Column(name = "user_email", length = 100, nullable = false, unique = true)
	private String userEmail;

	/**
	 * 사용자 이름
	 * - 실명
	 * - 최대 50자
	 * - NOT NULL 제약 조건
	 * - 예약 시 표시되는 이름
	 * - 한글, 영문만 입력 가능 (SignupRequestDto에서 검증)
	 */
	@Column(name = "user_name", length = 50, nullable = false)
	private String userName;

	/**
	 * 사용자 비밀번호 (암호화됨)
	 * - BCrypt 알고리즘으로 암호화하여 저장
	 * - 최대 255자 (BCrypt 해시 길이 고려)
	 * - NOT NULL 제약 조건
	 * - 평문 저장 금지
	 * - 영문, 숫자, 특수문자 각각 최소 1개씩 포함 필수
	 * - PasswordService를 통해 암호화 처리
	 */
	@Column(name = "user_pwd", length = 255, nullable = false)
	private String userPwd;

	/**
	 * 등록 일시
	 * - 사용자 계정 생성 시각
	 * - @CreationTimestamp로 자동 설정
	 * - NOT NULL 제약 조건
	 * - 변경 불가
	 */
	@CreationTimestamp
	@Column(name = "reg_date", nullable = false)
	private LocalDateTime regDate;

	/**
	 * 수정 일시
	 * - 사용자 정보 마지막 수정 시각
	 * - @UpdateTimestamp로 자동 갱신
	 * - NULL 허용 (최초 생성 시)
	 * - 수정 시마다 자동 업데이트
	 */
	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	/**
	 * 이메일 수신 동의 여부
	 * - 예약 관련 이메일 알림 수신 동의
	 * - NOT NULL 제약 조건
	 * - true: 수신 동의
	 * - false: 수신 거부
	 * - 예약 승인/거부 알림 발송 여부 결정
	 */
	@Column(name = "agree_email", nullable = false)
	private Boolean agreeEmail;

	/**
	 * 사용자 역할 (다대일 관계)
	 * - UserRole 엔티티와 ManyToOne 관계
	 * - 일반 사용자: roleId = 3
	 * - LAZY 로딩으로 성능 최적화
	 * - NOT NULL 제약 조건
	 * - 향후 권한별 기능 분기 처리 가능
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id", nullable = false)
	private UserRole userRole;

	/**
	 * 소속 기업 (다대일 관계)
	 * - UserCompany 엔티티와 ManyToOne 관계
	 * - 사용자의 소속 기업 정보
	 * - LAZY 로딩으로 성능 최적화
	 * - NOT NULL 제약 조건
	 * - 회원가입 시 필수 입력
	 * - 기업 단위 예약 관리 및 통계에 사용
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id", nullable = false)
	private UserCompany userCompany;

	/**
	 * 토큰 버전
	 * - JWT Refresh Token 무효화를 위한 버전 관리
	 * - 비밀번호 변경, 보안 사고 발생 시 증가
	 * - 기본값 1
	 * - NOT NULL 제약 조건
	 * - 토큰 재발급 시 현재 버전과 비교하여 유효성 검증
	 * - 버전이 다르면 기존 토큰 모두 무효화
	 * - AuthService의 refresh 메서드에서 검증
	 */
	@Column(name = "token_ver", nullable = false)
	private Integer tokenVer = 1;
}
