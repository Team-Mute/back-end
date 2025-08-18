package Team_Mute.back_end.domain.member.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CorpNameSearchRequestDto {
	@NotBlank(message = "기업명은 필수 입력 항목입니다.")
	@Size(min = 1, message = "기업명은 한 글자 이상 입력해야 합니다.")
	private String corpNm;

	@Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
	private Integer pageNo;
}
