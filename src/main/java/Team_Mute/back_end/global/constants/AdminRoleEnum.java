package Team_Mute.back_end.global.constants;

/**
 * 관리자의 시스템 권한을 정의하는 Enum 클래스입니다.
 * 각 권한은 불변의 ID 값을 가지며, 이 값은 관리자 인증 및 권한 확인에 사용됩니다.
 */
public enum AdminRoleEnum {

	/**
	 * 마스터 관리자 (Master) - 계정 생성 관리자
	 */
	ROLE_MASTER(0, "마스터 관리자"),

	/**
	 * 2차 승인자 (Approver)
	 */
	ROLE_SECOND_APPROVER(1, "2차 승인자"),

	/**
	 * 1차 승인자 (Manager)
	 */
	ROLE_FIRST_APPROVER(2, "1차 승인자"),


	/**
	 * 사용자 (customer)
	 */
	ROLE_USER(3, "사용자");

	private final Integer id;
	private final String description;

	/**
	 * AdminRole의 생성자입니다.
	 *
	 * @param id          관리자 권한 ID
	 * @param description 관리자 권한에 대한 설명
	 */
	AdminRoleEnum(Integer id, String description) {
		this.id = id;
		this.description = description;
	}

	/**
	 * 관리자 권한 ID를 반환합니다.
	 *
	 * @return 관리자 권한 ID (Long)
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * 관리자 권한명를 반환합니다.
	 *
	 * @return 관리자 권한명
	 */
	public String getDescription() {
		return description;
	}
}
