package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dm.dracolich.mtgLibrary.dto.enums.Color;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeckStatsDto(
        @JsonProperty("deck_id") String deckId,
        @JsonProperty("card_count") Integer cardCount,
        @JsonProperty("land_count") Integer landCount,
        @JsonProperty("mana_curve") Map<Integer, Integer> manaCurve,
        @JsonProperty("color_pie") Map<Color, Double> colorPie,
        @JsonProperty("type_breakdown") Map<String, Integer> typeBreakdown,
        @JsonProperty("average_cmc") Double averageCmc,
        List<String> warnings) { }
