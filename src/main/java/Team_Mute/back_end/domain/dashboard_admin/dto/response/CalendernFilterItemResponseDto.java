package Team_Mute.back_end.domain.dashboard_admin.dto.response;

import Team_Mute.back_end.global.constants.ReservationStatusEnum;
import lombok.Builder;
import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 캘린더 커스터마이징 화면 구성을 위해 사용되는 필터링 항목 응답 DTO
 * - 예약 상태 ID (Integer)와 커스텀 플래그 이름 (String)을 모두 String 타입 'id'로 통합하여 클라이언트(타입스크립트)의 타입 안정성과 UI 렌더링 편의성을 높임
 * - 'type' 필드를 통해 해당 'id'의 실제 용도를 구분합니다.
 */
@Getter
@Builder
// null 값 필드는 JSON 출력에서 제외 (ID가 없는 항목은 Label만, Label이 없는 항목은 ID만 출력)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CalendernFilterItemResponseDto {
	/**
	 * 필터링 항목의 고유 식별자 (Integer 또는 String)
	 * - @JsonInclude(JsonInclude.Include.NON_NULL)에 의해 이 필드는 항상 출력
	 * - Integer 또는 String 값을 담기 위해 Object 타입 사용
	 */
	private final Integer id;

	/**
	 * 필터링 항목의 사용자 친화적인 설명 (UI 표시용)
	 */
	private final String description;

	/**
	 * 해당 항목의 유형을 구분하는 식별자
	 * - "STATUS": 예약 상태 (statusIds 파라미터로 전송 필요)
	 * - "FLAG": 커스텀 플래그 (isEmergency, isShinha 개별 파라미터로 전송 필요)
	 */
	private final String type;

	/**
	 * [팩토리 메서드] ReservationStatusEnum 항목을 DTO로 변환
	 * * @param statusEnum DB에 정의된 예약 상태 Enum 값
	 *
	 * @return type이 "STATUS"인 DTO
	 */
	public static CalendernFilterItemResponseDto fromStatusEnum(ReservationStatusEnum statusEnum) {
		return CalendernFilterItemResponseDto.builder()
			.id(statusEnum.getId())
			.description(statusEnum.getDescription())
			.type("STATUS")
			.build();
	}
}
