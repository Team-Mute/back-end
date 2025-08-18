package Team_Mute.back_end.domain.member.util;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import Team_Mute.back_end.domain.member.dto.external.corp_api.CorpApiItemDto;

public class CustomItemDeserializer extends JsonDeserializer<List<CorpApiItemDto>> {

	@Override
	public List<CorpApiItemDto> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		ObjectMapper mapper = (ObjectMapper)p.getCodec();
		if (p.getCurrentToken() == JsonToken.START_ARRAY) {
			return mapper.readValue(p,
				mapper.getTypeFactory().constructCollectionType(List.class, CorpApiItemDto.class));
		}
		CorpApiItemDto singleItem = mapper.readValue(p, CorpApiItemDto.class);
		return Collections.singletonList(singleItem);
	}
}
