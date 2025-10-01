package Team_Mute.back_end.domain.space_admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 공간 삭제 응답 DTO
 * - 특정 공간(Space)이 정상적으로 삭제되었을 때 API 응답 형식으로 사용
 * - 삭제 결과 메시지와 삭제된 공간의 ID를 함께 전달
 * - @Getter + @AllArgsConstructor 조합으로 간결하게 정의
 * <p>
 * 예시 응답(JSON)
 * {
 * "message": "공간 삭제 완료",
 * "spaceId": 10
 * }
 */
@Getter
@AllArgsConstructor
public class DeleteSpaceResponseDto {
	private final String message;
	private final Integer spaceId;
}

