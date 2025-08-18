package Team_Mute.back_end.domain.space_admin.dto;

import Team_Mute.back_end.domain.space_admin.entity.SaveStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

@Getter
@Setter
public class SpaceCreateRequest {
	private String spaceName;
	private Integer locationId;
	private String spaceDescription;
	private Integer spaceCapacity;
	private Boolean spaceIsAvailable;
	private String regionName;
	private Integer regionId;
	private Integer categoryId;
	private String categoryName;
	private List<String> tagNames;
	private Integer userId;
	private String imageUrl;

	@Size(max = 5000) // 길이 여유
	private String reservationWay;

	@Size(max = 5000) // 길이 여유
	private String spaceRules;

	@NotNull(message = "saveStatus는 필수입니다")
	private SaveStatus saveStatus;

	// 운영시간: 1~7(월~일) 요일, HH:mm 형식
	private List<OperationItem> operations;

	// 휴무일: ISO-8601 (예: 2025-08-20T00:00:00)
	private List<ClosedItem> closedDays;

	@Getter
	@Setter
	public static class OperationItem {
		@NotNull
		@Min(1)
		@Max(7)
		private Integer day;             // 1=월 ~ 7=일

		// "HH:mm" 형식만 허용
		@JsonFormat(pattern = "HH:mm")
		private LocalTime from;       // isOpen=true면 필수

		@JsonFormat(pattern = "HH:mm")
		private LocalTime to;       // isOpen=true면 필수

		private Boolean isOpen;    // true/false
	}

	@Getter
	@Setter
	public static class ClosedItem {
		private LocalDateTime from;      // "2025-08-25T00:00:00"
		private LocalDateTime to;        // "2025-08-25T23:59:59"
	}

	// 월~일 7개/중복 없음/시간 정합성 검증 (@AssertTrue 커스텀 검증)
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

