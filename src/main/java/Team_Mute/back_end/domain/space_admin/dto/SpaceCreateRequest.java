package Team_Mute.back_end.domain.space_admin.dto;

import Team_Mute.back_end.domain.space_admin.entity.SaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SpaceCreateRequest {
	private String spaceName;
	private String spaceLocation;
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
		private Integer day;      // 1=월 ~ 7=일
		private String from;     // "09:00"
		private String to;       // "18:00"
		private Boolean isOpen;   // true/false
	}

	@Getter
	@Setter
	public static class ClosedItem {
		private String from;      // "2025-08-25T00:00:00"
		private String to;        // "2025-08-25T23:59:59"
	}
}

