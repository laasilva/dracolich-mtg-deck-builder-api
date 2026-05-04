package dm.dracolich.deckBuilder.dto;

import jakarta.validation.constraints.Min;
import lombok.NonNull;

public record UpdateCardRequest(@NonNull @Min(1) Integer count) { }
