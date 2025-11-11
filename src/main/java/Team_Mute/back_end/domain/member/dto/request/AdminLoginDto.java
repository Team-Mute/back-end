package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 로그인 요청 및 응답 DTO를 포함하는 래퍼 클래스
 * Request와 Response를 내부 클래스로 포함하여 관련 DTO를 그룹화
 * AdminAuthController의 login API에서 사용
 *
 * @author Team Mute
 * @since 1.0
 */
public class AdminLoginDto {

	/**
	 * 관리자 로그인 요청 DTO (Record 타입)
	 * Java 17의 Record를 사용하여 불변 데이터 클래스 정의
	 *
	 * @param email 관리자 이메일 주소 (필수)
	 * @param password 관리자 비밀번호 (필수)
	 */
	public record Request(
		@NotBlank String email,
		@NotBlank String password
	) {
	}

	/**
	 * 관리자 로그인 응답 DTO
	 * 로그인 성공 시 클라이언트에 전달할 정보 포함
	 */
	public static class Response {
		/**
		 * JWT Access Token
		 * - API 요청 시 Authorization 헤더에 포함하여 사용
		 */
		private String accessToken;

		/**
		 * 관리자 역할 ID
		 * - 0: 마스터 관리자
		 * - 1: 2차 승인자
		 * - 2: 1차 승인자
		 * - 프론트엔드에서 권한별 UI 분기 처리에 사용
		 */
		private Integer roleId;

		/**
		 * Response 생성자
		 *
		 * @param accessToken JWT Access Token
		 * @param roleId 관리자 역할 ID
		 */
		public Response(String accessToken, Integer roleId) {
			this.accessToken = accessToken;
			this.roleId = roleId;
		}

		/**
		 * Access Token Getter
		 *
		 * @return JWT Access Token
		 */
		public String getAccessToken() {
			return accessToken;
		}

		/**
		 * Role ID Getter
		 *
		 * @return 관리자 역할 ID
		 */
		public Integer getRoleId() {
			return roleId;
		}
	}
}
