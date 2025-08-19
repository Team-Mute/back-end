package Team_Mute.back_end.domain.space_admin.dto;

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

@Getter
@Setter
@Schema(description = "공간 등록 JSON")
public class SpaceCreateRequest {
	@NotBlank
	@Schema(example = "공간명")
	private String spaceName;

	@NotBlank
	@Schema(example = "쾌적한 공간입니다.")
	private String spaceDescription;

	@NotNull
	@Min(1)
	@Schema(example = "1")
	private Integer locationId;

	@NotNull
	@Min(1)
	@Schema(example = "50")
	private Integer spaceCapacity;

	@NotNull
	@Min(1)
	@Schema(example = "1")
	private Integer regionId;

	@NotNull
	@Min(1)
	@Schema(example = "1")
	private Integer categoryId;

	@NotNull
	@Schema(example = "true")
	private Boolean spaceIsAvailable;

	@Schema(example = "[\"TV\",\"화이트보드\",\"WIFI\"]")
	private List<String> tagNames;

	@NotNull
	@Min(1)
	@Schema(example = "1")
	private Integer userId;

	@Size(max = 5000) // 길이 여유
	@Schema(example = "웹 신청 후 관리자 승인")
	private String reservationWay;

	@Size(max = 5000) // 길이 여유
	@Schema(example = "실내 흡연 금지, 음식물 반입 금지")
	private String spaceRules;

	// 운영시간: 1~7(월~일) 요일, HH:mm 형식
	@NotNull
	private List<OperationItem> operations;

	// 휴무일: ISO-8601 (예: 2025-08-20T00:00:00)
	@NotNull
	private List<ClosedItem> closedDays;


	//private String regionName;
	//private String categoryName;
	//private String imageUrl;

	@Getter
	@Setter
	public static class OperationItem {
		@NotNull
		@Min(1)
		@Max(7)
		private Integer day;             // 1=월 ~ 7=일

		// "HH:mm" 형식만 허용
		@JsonFormat(pattern = "HH:mm")
		@Schema(type = "string", example = "09:00")
		private LocalTime from;       // isOpen=true면 필수

		@JsonFormat(pattern = "HH:mm")
		@Schema(type = "string", example = "18:00")
		private LocalTime to;       // isOpen=true면 필수

		@NotNull
		@Schema(example = "true")
		private Boolean isOpen;    // true/false
	}

	@Getter
	@Setter
	public static class ClosedItem {
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
		@Schema(type = "string", example = "2025-09-15T00:00:00")
		private LocalDateTime from;

		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
		@Schema(type = "string", example = "2025-09-15T23:59:59")
		private LocalDateTime to;
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

