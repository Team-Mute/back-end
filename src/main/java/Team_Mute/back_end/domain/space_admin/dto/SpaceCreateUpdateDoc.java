package Team_Mute.back_end.domain.space_admin.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.web.multipart.MultipartFile;

@Schema(name = "SpaceCreateUpdateDoc", description = "multipart/form-data 본문")
public class SpaceCreateUpdateDoc {
	private SpaceCreateRequest space;      // ← 객체 타입
	private MultipartFile[] images;        // ← 문서용은 배열 권장 (UI 인식이 안정적)

	// 여기 example이 Swagger UI의 Edit Value에 기본으로 뜹니다.
	@Schema(
		implementation = SpaceCreateRequest.class,
		description = "공간 정보(JSON)",
		example = """
			{
			  "spaceName": "명동 신한스퀘어브릿지 6층 2A",
			  "spaceDescription": "50명 수용 가능한 행사장입니다.",
			  "spaceCapacity": 50,
			  "spaceIsAvailable": true,
			  "regionId": 1,
			  "categoryId": 1,
			  "locationId": 1,
			  "tagNames": ["TV", "화이트보드", "WIFI"],
			  "userId": 1,
			  "reservationWay": "웹 신청 후 관리자 승인",
			  "spaceRules": "실내 흡연 금지, 음식물 반입 금지",
			  "operations": [
			    { "day": 1, "from": "09:00", "to": "18:00", "isOpen": true },
			    { "day": 2, "from": "09:00", "to": "18:00", "isOpen": true },
			    { "day": 3, "from": "09:00", "to": "18:00", "isOpen": true },
			    { "day": 4, "from": "09:00", "to": "18:00", "isOpen": true },
			    { "day": 5, "from": "09:00", "to": "18:00", "isOpen": true },
			    { "day": 6, "from": "10:00", "to": "14:00", "isOpen": true },
			    { "day": 7, "from": "00:00", "to": "00:00", "isOpen": false }
			  ],
			  "closedDays": [
			    { "from": "2025-09-15T00:00:00", "to": "2025-09-15T23:59:59" },
			    { "from": "2025-10-03T00:00:00", "to": "2025-10-03T23:59:59" }
			  ]
			}
			"""
	)
	public SpaceCreateRequest getSpace() {
		return space;
	}

	public void setSpace(SpaceCreateRequest v) {
		this.space = v;
	}

	//  파일 선택 UI 강제
	@ArraySchema(schema = @Schema(type = "string", format = "binary"))
	public MultipartFile[] getImages() {
		return images;
	}

	public void setImages(MultipartFile[] v) {
		this.images = v;
	}
}
