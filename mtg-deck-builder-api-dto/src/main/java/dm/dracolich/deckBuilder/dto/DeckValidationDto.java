package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dm.dracolich.mtgLibrary.dto.enums.Format;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeckValidationDto(
        @JsonProperty("deck_id") String deckId,
        Format format,
        Boolean valid,
        List<ValidationViolationDto> violations) { }
