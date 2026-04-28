package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeckAnalysisDto(
        DeckValidationDto validation,
        DeckStatsDto stats) { }
