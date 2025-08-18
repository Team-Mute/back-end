package Team_Mute.back_end.domain.member.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorpNameSearchResponseDto {
	// 검색된 기업명 목록
	private List<String> item;

	// 마지막으로 검색한 페이지 번호
	private int pageNo;
}
