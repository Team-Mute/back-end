package Team_Mute.back_end.domain.space_admin.dto;

import Team_Mute.back_end.domain.space_admin.dto.request.SpaceCreateRequestDto;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 공간 수정 요청 DTO
 * - Swagger 문서화용 객체
 * - 관리자가 기존 공간 정보를 수정할 때 요청 본문(body)에 매핑됨
 * - 공간명, 설명, 이미지, 카테고리, 태그 등 변경 가능한 필드 포함
 * - 부분 수정(Patch)와 전체 수정(Put) 시 공통적으로 활용 가능
 */
@Schema(name = "SpaceUpdateDoc", description = "multipart/form-data 본문")
public class SpaceUpdateDoc {
	private SpaceCreateRequestDto space;    // 객체 타입
	private MultipartFile[] images;        // 문서용은 배열 권장 (UI 인식이 안정적)
	private List<String> keepUrlsOrder;   // 최종 순서(기존 URL + "new:i")

	// 여기 example이 Swagger UI의 Edit Value에 기본으로 뜹니다.
	@Schema(
		implementation = SpaceCreateRequestDto.class,
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
			  "adminId": 3,
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
	public SpaceCreateRequestDto getSpace() {
		return space;
	}

	public void setSpace(SpaceCreateRequestDto v) {
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

	// 최종 순서(기존 URL + "new:i") — 예: ["https://.../E1.jpg","new:0","new:1","https://.../E2.jpg","new:2"]
	@ArraySchema(schema = @Schema(type = "string"))
	@Schema(
		description = """
			최종 이미지 순서 배열(기존 URL + "new:i" 혼합)
			- 업로드한 새 파일 수 == new:0..new:(n-1) 토큰과 정확히 일치해야 함
			- 빈 배열([])이면 모든 이미지 삭제로 간주
			예) ["https://.../E1.jpg","new:0","new:1","https://.../E2.jpg","new:2"]
			""",
		example = "[\"https://.../E1.jpg\",\"new:0\",\"new:1\",\"https://.../E2.jpg\",\"new:2\"]"
	)
	public List<String> getKeepUrlsOrder() {
		return keepUrlsOrder;
	}

	public void setKeepUrlsOrder(List<String> keepUrlsOrder) {
		this.keepUrlsOrder = keepUrlsOrder;
	}
}
