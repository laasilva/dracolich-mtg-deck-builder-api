package dm.dracolich.deckBuilder.core.text;

import dm.dracolich.deckBuilder.dto.enums.CardCategory;

public record ParsedLine(
        int lineNumber,
        String originalLine,
        Integer count,
        String cardName,
        Section section,
        String parseError) {

    public boolean isValid() {
        return parseError == null && cardName != null && count != null;
    }

    public enum Section {
        COMMANDER,
        MAINBOARD,
        SIDEBOARD,
        MAYBEBOARD;

        public CardCategory toCategory() {
            return switch (this) {
                case MAINBOARD -> CardCategory.MAINBOARD;
                case SIDEBOARD -> CardCategory.SIDEBOARD;
                case MAYBEBOARD -> CardCategory.MAYBE_BOARD;
                case COMMANDER -> null;
            };
        }
    }
}
