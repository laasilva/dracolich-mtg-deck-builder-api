package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dm.dracolich.deckBuilder.dto.enums.DeckStatus;
import dm.dracolich.deckBuilder.dto.enums.Visibility;
import dm.dracolich.mtgLibrary.dto.enums.Format;

public record UpdateDeckRequest(String name,
                                String description,
                                Format format,
                                @JsonProperty("deck_status") DeckStatus deckStatus,
                                Visibility visibility) { }
