package dm.dracolich.deckBuilder.dto.error;

import dm.dracolich.forge.error.ErrorCode;
import lombok.Getter;

@Getter
public enum ErrorCodes implements ErrorCode {
    DMD030("DMD030", "MTG Library lookup failed: %s"),
    DMD031("DMD031", "MTG Library request rejected"),
    DMD032("DMD032", "MTG Library API unavailable"),
    DMD033("DMD033", "%s: %s not found: %s"),
    DMD034("DMD034", "User not authorized to perform this action."),
    DMD035("DMD035", "Deck import failed: %s"),
    DMD036("DMD036", "Deck %s has already been claimed");

    private final String code;
    private final String message;

    ErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String format(String... args) {
        return String.format(message, args);
    }
}
