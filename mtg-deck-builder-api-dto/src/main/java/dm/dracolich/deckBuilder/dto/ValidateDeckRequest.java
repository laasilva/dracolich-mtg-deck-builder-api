package dm.dracolich.deckBuilder.dto;

import dm.dracolich.mtgLibrary.dto.enums.Format;
import jakarta.validation.Valid;
import lombok.NonNull;

import java.util.Set;

public record ValidateDeckRequest(
        @NonNull Format format,
        @Valid CardRequest commander,
        @Valid @NonNull Set<CardRequest> cards) { }
