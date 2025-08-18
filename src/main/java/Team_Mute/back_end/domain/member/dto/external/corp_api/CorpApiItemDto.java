package Team_Mute.back_end.domain.member.dto.external.corp_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CorpApiItemDto {
	private String corpNm;
	private String bzno;
	private String enpEstbDt;
}
