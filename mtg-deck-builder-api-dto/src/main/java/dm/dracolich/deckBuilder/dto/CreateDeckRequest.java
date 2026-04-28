package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dm.dracolich.deckBuilder.dto.enums.DeckStatus;
import dm.dracolich.deckBuilder.dto.enums.Visibility;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import jakarta.validation.Valid;
import lombok.NonNull;

import java.util.Set;

public record CreateDeckRequest(@NonNull Format format,
                                @JsonProperty("copied_from_id") String copiedFromId,
                                @NonNull String name,
                                String description,
                                @JsonProperty("deck_status") DeckStatus deckStatus,
                                @Valid CardRequest commander,
                                @Valid @NonNull Set<CardRequest> cards,
                                Visibility visibility) { }
