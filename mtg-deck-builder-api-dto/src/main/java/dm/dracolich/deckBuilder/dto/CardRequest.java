package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dm.dracolich.deckBuilder.dto.enums.CardCategory;
import jakarta.validation.constraints.Min;
import lombok.NonNull;

public record CardRequest(@JsonProperty("card_id") @NonNull String cardId,
                          @JsonProperty("card_category") CardCategory cardCategory,
                          @NonNull @Min(1) Integer count) {
}
