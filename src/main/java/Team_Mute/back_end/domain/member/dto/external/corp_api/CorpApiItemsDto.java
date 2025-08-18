package Team_Mute.back_end.domain.member.dto.external.corp_api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CorpApiItemsDto {
	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "item")
	private List<CorpApiItemDto> item;
}
