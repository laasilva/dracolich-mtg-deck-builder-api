package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import lombok.NonNull;

public record ImportValidateRequest(
        @NonNull Format format,
        @NonNull @JsonProperty("deck_text") String deckText) { }
