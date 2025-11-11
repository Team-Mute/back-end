package Team_Mute.back_end.domain.member.dto.response;

import java.time.LocalDateTime;

import Team_Mute.back_end.domain.member.entity.User;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 정보 조회 응답 DTO
 * 사용자가 본인의 계정 정보를 조회할 때 클라이언트에 반환되는 데이터
 * UserController의 getUserInfo API 응답으로 사용
 * 민감한 정보(비밀번호)는 제외하고 필요한 정보만 포함
 * Builder 패턴과 정적 팩토리 메서드를 사용하여 Entity를 DTO로 변환
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
@Builder
public class UserInfoResponseDto {
	/**
	 * 사용자 ID
	 * - 데이터베이스 Primary Key
	 * - 사용자 고유 식별자
	 */
	private Long userId;

	/**
	 * 사용자 역할 ID
	 * - 일반 사용자: 3
	 * - 향후 권한별 기능 분기 처리에 사용 가능
	 */
	private Integer roleId;

	/**
	 * 소속 기업 ID
	 * - UserCompany 테이블의 Primary Key
	 * - 소속 기업 정보 조회에 사용
	 */
	private Integer companyId;

	/**
	 * 사용자 이메일 주소
	 * - 로그인 ID로 사용
	 * - 계정 식별자
	 */
	private String userEmail;

	/**
	 * 사용자 이름
	 * - 실명
	 * - 예약 시 표시되는 이름
	 */
	private String userName;

	/**
	 * 회원가입 일시
	 * - 계정 생성 시각
	 * - ISO 8601 형식으로 직렬화
	 */
	private LocalDateTime regDate;

	/**
	 * 정보 수정 일시
	 * - 마지막으로 계정 정보를 수정한 시각
	 * - 수정 이력 추적에 사용
	 */
	private LocalDateTime updDate;

	/**
	 * 이메일 수신 동의 여부
	 * - true: 예약 관련 이메일 알림 수신 동의
	 * - false: 이메일 알림 수신 거부
	 * - 마이페이지에서 변경 가능
	 */
	private Boolean agreeEmail;

	/**
	 * 소속 기업명
	 * - UserCompany 엔티티에서 추출한 기업명
	 * - 회원가입 시 입력한 기업명
	 */
	private String companyName;

	/**
	 * 역할명
	 * - UserRole 엔티티에서 추출한 역할명 (Customer)
	 * - 화면에 표시할 역할 이름
	 */
	private String roleName;

	/**
	 * User 엔티티를 UserInfoResponseDto로 변환하는 정적 팩토리 메서드
	 * - 서비스 레이어에서 조회한 User 엔티티를 DTO로 변환
	 * - Builder 패턴을 사용하여 가독성 향상
	 * - 연관 관계(UserCompany, UserRole)에서 필요한 정보 추출
	 * - 비밀번호 등 민감한 정보는 제외
	 *
	 * @param user 변환할 User 엔티티
	 * @return UserInfoResponseDto 변환된 DTO 객체
	 */
	public static UserInfoResponseDto fromEntity(User user) {
		return UserInfoResponseDto.builder()
			.userId(user.getUserId())
			.roleId(user.getUserRole().getRoleId())
			.companyId(user.getUserCompany().getCompanyId())
			.userEmail(user.getUserEmail())
			.userName(user.getUserName())
			.regDate(user.getRegDate())
			.updDate(user.getUpdDate())
			.agreeEmail(user.getAgreeEmail())
			.companyName(user.getUserCompany().getCompanyName())
			.roleName(user.getUserRole().getRoleName())
			.build();
	}
}
