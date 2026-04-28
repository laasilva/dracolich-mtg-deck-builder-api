package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dm.dracolich.deckBuilder.dto.enums.DeckStatus;
import dm.dracolich.deckBuilder.dto.enums.Visibility;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import lombok.NonNull;

public record ImportDeckRequest(
        @NonNull Format format,
        @NonNull String name,
        String description,
        @JsonProperty("deck_status") DeckStatus deckStatus,
        Visibility visibility,
        @NonNull @JsonProperty("deck_text") String deckText) { }
