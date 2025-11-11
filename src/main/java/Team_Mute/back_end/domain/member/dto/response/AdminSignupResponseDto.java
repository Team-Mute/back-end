package Team_Mute.back_end.domain.member.dto.response;

import Team_Mute.back_end.domain.member.entity.Admin;
import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 회원가입 응답 DTO
 * 마스터 관리자가 새로운 관리자 계정을 생성할 때 클라이언트에 반환되는 데이터
 * AdminController의 adminSignUp API 응답으로 사용
 * 생성된 관리자의 기본 정보를 포함하며 비밀번호는 제외
 * Builder 패턴과 정적 팩토리 메서드를 사용하여 Entity를 DTO로 변환
 *
 * @author Team Mute
 * @since 1.0
 */
@Getter
@Builder
public class AdminSignupResponseDto {
	/**
	 * 생성된 관리자 ID
	 * - 데이터베이스에서 자동 생성된 Primary Key
	 * - 관리자 고유 식별자
	 */
	private Long adminId;

	/**
	 * 생성된 관리자 이메일 주소
	 * - 로그인 ID로 사용
	 * - 중복 불가
	 */
	private String adminEmail;

	/**
	 * 생성된 관리자 이름
	 * - 실명
	 * - 시스템 내 표시 이름
	 */
	private String adminName;

	/**
	 * 생성된 관리자 역할 ID
	 * - 0: 마스터 관리자
	 * - 1: 2차 승인자
	 * - 2: 1차 승인자
	 */
	private Integer roleId;

	/**
	 * Admin 엔티티를 AdminSignupResponseDto로 변환하는 정적 팩토리 메서드
	 * - 서비스 레이어에서 생성된 Admin 엔티티를 DTO로 변환
	 * - Builder 패턴을 사용하여 가독성 향상
	 * - 엔티티의 연관 관계(UserRole)에서 roleId 추출
	 *
	 * @param admin 변환할 Admin 엔티티
	 * @return AdminSignupResponseDto 변환된 DTO 객체
	 */
	public static AdminSignupResponseDto fromEntity(Admin admin) {
		return AdminSignupResponseDto.builder()
			.adminId(admin.getAdminId())
			.adminEmail(admin.getAdminEmail())
			.adminName(admin.getAdminName())
			.roleId(admin.getUserRole().getRoleId())
			.build();
	}
}
