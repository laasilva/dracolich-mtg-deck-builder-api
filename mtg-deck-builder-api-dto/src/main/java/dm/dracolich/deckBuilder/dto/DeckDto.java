package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dm.dracolich.deckBuilder.dto.enums.DeckStatus;
import dm.dracolich.deckBuilder.dto.enums.Visibility;
import dm.dracolich.mtgLibrary.dto.GameFormatDto;
import dm.dracolich.mtgLibrary.dto.enums.Color;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeckDto {
    private String id;
    @JsonProperty("copied_from_id")
    private String copiedFromId; // if deck was copied
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("anon_id")
    private String anonId;
    private AuditDto audit;
    private String name;
    private String description;
    private Set<Color> colors; // color id for commander
    private Format format;
    private DeckStatus status;
    private List<DeckCardDto> commander; // only if commander deck - some can have partners
    private List<DeckCardDto> cards = new ArrayList<>(); // limit size according to format
    @JsonProperty("favorites_count")
    private Long favoritesCount;
    private Visibility visibility;
    @JsonProperty("ai_session_id")
    private String aiSessionId;
    @JsonProperty("card_count")
    private Integer cardCount;
    @JsonProperty("mainboard_count")
    private Integer mainboardCount;
    @JsonProperty("sideboard_count")
    private Integer sideboardCount;
    @JsonProperty("maybeboard_count")
    private Integer maybeboardCount;
}
