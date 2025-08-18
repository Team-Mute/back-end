package Team_Mute.back_end.domain.member.dto.external.corp_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")
public class CorpApiResponseDto {
	private CorpApiHeaderDto header;
	private CorpApiBodyDto body;
}
