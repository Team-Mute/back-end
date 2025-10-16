package Team_Mute.back_end.global.constants;

/**
 * 예약 상태를 정의하는 Enum 클래스
 * 각 상태는 불변의 ID 값을 가지며, 해당 값은 DB의 예약 상태 코드와 매핑됩니다.
 */
public enum ReservationStatusEnum {

	/**
	 * 1차 승인 대기 상태 (ID: 1)
	 */
	WAITING_FIRST_APPROVAL(1, "1차 승인 대기"),

	/**
	 * 2차 승인 대기 상태 (ID: 2)
	 */
	WAITING_SECOND_APPROVAL(2, "2차 승인 대기"),

	/**
	 * 최종 승인 완료 상태 (ID: 3)
	 */
	FINAL_APPROVAL(3, "최종 승인 완료"),

	/**
	 * 반려 상태 (ID: 4)
	 */
	REJECTED_STATUS(4, "반려"),

	/**
	 * 이용 완료 상태 (ID: 5)
	 */
	USER_COMPLETED(5, "이용 완료"),

	/**
	 * 예약 취소 상태 (ID: 6)
	 */
	CANCELED_STATUS(6, "예약 취소");

	private final Integer id;
	private final String description;

	/**
	 * ReservationStatus의 생성자
	 *
	 * @param id          예약 상태 ID
	 * @param description 예약 상태에 대한 설명
	 */
	ReservationStatusEnum(Integer id, String description) {
		this.id = id;
		this.description = description;
	}

	/**
	 * 예약 상태 ID를 반환
	 * 이 ID는 필터링 로직에서 예약 상태를 비교하는 데 사용
	 *
	 * @return 예약 상태 ID (Integer)
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * 예약 상태명을 반환
	 *
	 * @return 예약 상태명 (String)
	 */
	public String getDescription() {
		return description;
	}
}
