package Team_Mute.back_end.domain.space_admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 공간 등록 요청 DTO
 * - 관리자 화면에서 공간 생성 시 사용하는 입력 모델
 * - 유효성 검사(Validation) 어노테이션으로 필수값/형식을 검증
 * - 운영시간(요일별)과 휴무일(기간)까지 함께 전달
 */
@Getter
@Setter
@Schema(description = "공간 등록 JSON")
public class SpaceCreateRequestDto {
	/**
	 * 공간명 (필수)
	 */
	@NotBlank
	@Schema(example = "공간명")
	private String spaceName;

	/**
	 * 공간 설명 (필수, TEXT 가능)
	 */
	@NotBlank
	@Schema(example = "쾌적한 공간입니다.")
	private String spaceDescription;

	/**
	 * 위치 ID (FK: tb_locations.location_id)
	 */
	@NotNull
	@Min(1)
	@Schema(example = "1")
	private Integer locationId;

	/**
	 * 수용 인원 (필수, 최소 1명)
	 */
	@NotNull
	@Min(1)
	@Schema(example = "50")
	private Integer spaceCapacity;

	/**
	 * 지역 ID (숫자 코드, 예: 1=서울 …)
	 */
	@NotNull
	@Min(1)
	@Schema(example = "1")
	private Integer regionId;

	/**
	 * 카테고리 ID (숫자 코드)
	 * - 예: 1=미팅룸, 2=행사장, ...
	 * - FK: tb_space_categories.category_id
	 */
	@NotNull
	@Min(1)
	@Schema(example = "1")
	private Integer categoryId;

	/**
	 * 활성화 여부 (true=예약 가능)
	 */
	@NotNull
	@Schema(example = "true")
	private Boolean spaceIsAvailable;

	/**
	 * 태그명 목록 (예: TV, 화이트보드, WIFI)
	 */
	@Schema(example = "[\"TV\",\"화이트보드\",\"WIFI\"]")
	private List<String> tagNames;

	/**
	 * 담당자 아이디
	 */
	@NotNull
	private Long adminId;

	/**
	 * 예약 방식 안내(선택, 길이 여유)
	 */
	@Size(max = 5000) // 길이 여유
	@Schema(example = "웹 신청 후 관리자 승인")
	private String reservationWay;

	/**
	 * 이용 수칙 안내(선택, 길이 여유)
	 */
	@Size(max = 5000) // 길이 여유
	@Schema(example = "실내 흡연 금지, 음식물 반입 금지")
	private String spaceRules;

	/**
	 * 운영시간(요일별) 목록
	 * - day: 1=월 ~ 7=일
	 * - isOpen=true면 from/to 필수(HH:mm)
	 * - 총 7개, 요일 중복 없음 (isValidOperations로 검증)
	 */
	@NotNull
	private List<OperationItem> operations;

	/**
	 * 휴무일(기간) 목록
	 * - from/to: ISO-8601 DateTime
	 * - 예: 2025-08-20T00:00:00 ~ 2025-08-20T23:59:59
	 */
	@NotNull
	private List<ClosedItem> closedDays;

	/**
	 * 요일 운영시간 항목
	 * - 1~7(월~일) 중 하나
	 * - isOpen=true → from/to 필수, 시간 형식 HH:mm
	 */
	@Getter
	@Setter
	public static class OperationItem {
		/**
		 * 요일 (1=월 ~ 7=일)
		 */
		@NotNull
		@Min(1)
		@Max(7)
		@Schema(description = "요일 (1=월요일, 2=화요일, …, 7=일요일)", example = "1")
		private Integer day;

		/**
		 * 시작 시각(HH:mm), isOpen=true일 때 필수
		 */
		@JsonFormat(pattern = "HH:mm")
		@Schema(type = "string", example = "09:00")
		private LocalTime from;

		/**
		 * 종료 시각(HH:mm), isOpen=true일 때 필수
		 */
		@JsonFormat(pattern = "HH:mm")
		@Schema(type = "string", example = "18:00")
		private LocalTime to;

		/**
		 * 운영 여부 (true=영업, false=휴무)
		 */
		@NotNull
		@Schema(example = "true")
		private Boolean isOpen;    // true/false
	}

	/**
	 * 휴무일 기간 항목
	 * - 동일 일자 내/여러 일자 범위 모두 표현 가능
	 */
	@Getter
	@Setter
	public static class ClosedItem {
		/**
		 * 시작 일시 (ISO-8601)
		 */
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
		@Schema(type = "string", example = "2025-09-15T00:00:00")
		private LocalDateTime from;

		/**
		 * 종료 일시 (ISO-8601)
		 */
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
		@Schema(type = "string", example = "2025-09-15T23:59:59")
		private LocalDateTime to;
	}

	/**
	 * 운영시간 유효성 검증
	 * - 요일은 1~7 각각 1건씩 총 7개
	 * - isOpen=true → from/to 필수, from==to 금지(0분 운영 금지)
	 * - 자정 넘김(from > to)은 정책에 따라 허용/금지 가능 (현재 주석 참고)
	 */
	@AssertTrue(message = "operations는 월(1)~일(7) 총 7개가 각각 1건씩 있어야 하며, 영업 요일은 시간 형식(HH:mm)이 유효해야 합니다.")
	private boolean isValidOperations() {
		if (operations == null || operations.size() != 7) return false;

		Set<Integer> seen = new HashSet<>();
		for (OperationItem o : operations) {
			if (o == null || o.getDay() == null) return false;
			int d = o.getDay();
			if (d < 1 || d > 7) return false;
			if (!seen.add(d)) return false; // 요일 중복 금지

			if (Boolean.TRUE.equals(o.getIsOpen())) {
				if (o.getFrom() == null || o.getTo() == null) return false;
				if (o.getFrom().equals(o.getTo())) return false; // 0분 운영 금지
				// from > to는 '자정 넘김'으로 허용하려면 여기서 통과시킴
				// 자정 넘김을 금지하려면: if (o.getFrom().isAfter(o.getTo())) return false;
			} else {
				// isOpen=false면 from/to가 null이어도 OK
			}
		}
		return seen.size() == 7;
	}
}

