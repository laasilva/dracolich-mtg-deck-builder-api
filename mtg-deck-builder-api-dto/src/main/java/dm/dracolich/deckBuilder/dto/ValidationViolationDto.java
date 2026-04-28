package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dm.dracolich.deckBuilder.dto.enums.ValidationSeverity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationViolationDto(
        String rule,
        String message,
        @JsonProperty("card_name") String cardName,
        ValidationSeverity severity) { }
