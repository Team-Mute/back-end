package Team_Mute.back_end.domain.dashboard_admin.dto;

/**
 * 대시보드 카운트와 같이 레이블(이름)과 값(건수) 쌍으로 구성된 항목을 표현하는 DTO
 */
public class SelectItemResponseDto {
	private String label;
	private long count;

	/**
	 * SelectItemResponseDto의 생성자
	 *
	 * @param label 항목의 이름(레이블) (예: "1차 승인 대기")
	 * @param count 해당 항목의 건수 (예: 7)
	 */
	public SelectItemResponseDto(String label, long count) {
		this.label = label;
		this.count = count;
	}

	// Getter and Setter

	/**
	 * 항목의 레이블을 반환
	 *
	 * @return 레이블 문자열
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * 항목의 레이블을 설정
	 *
	 * @param label 설정할 레이블 문자열
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * 항목의 건수를 반환합니다.
	 *
	 * @return 건수 (long 타입)
	 */
	public long getCount() {
		return count;
	}

	/**
	 * 항목의 건수를 설정
	 *
	 * @param count 설정할 건수 (long 타입)
	 */
	public void setCount(long count) {
		this.count = count;
	}
}
