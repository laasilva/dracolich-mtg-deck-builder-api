package dm.dracolich.deckBuilder.core.text;

import dm.dracolich.deckBuilder.data.entity.DeckCardEntity;
import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.dto.enums.CardCategory;

import java.util.Comparator;
import java.util.List;

public final class DeckTextExporter {

    private DeckTextExporter() {
    }

    public static String export(DeckEntity deck) {
        StringBuilder sb = new StringBuilder();

        appendSection(sb, filterByCategory(deck, CardCategory.MAINBOARD), null);

        if (deck.getCommander() != null && !deck.getCommander().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            appendSection(sb, deck.getCommander(), "// Commander");
        }

        List<DeckCardEntity> sideboard = filterByCategory(deck, CardCategory.SIDEBOARD);
        if (!sideboard.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            appendSection(sb, sideboard, "// Sideboard");
        }

        List<DeckCardEntity> maybeboard = filterByCategory(deck, CardCategory.MAYBE_BOARD);
        if (!maybeboard.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            appendSection(sb, maybeboard, "// Maybeboard");
        }

        return sb.toString();
    }

    private static void appendSection(StringBuilder sb, List<DeckCardEntity> cards, String header) {
        if (cards.isEmpty()) return;
        if (header != null) sb.append(header).append("\n");
        cards.stream()
                .sorted(Comparator.comparing(DeckCardEntity::getName, Comparator.nullsLast(String::compareTo)))
                .forEach(c -> {
                    int qty = c.getCount() != null ? c.getCount() : 1;
                    sb.append(qty).append(" ").append(c.getName() == null ? "" : c.getName()).append("\n");
                });
    }

    private static List<DeckCardEntity> filterByCategory(DeckEntity deck, CardCategory category) {
        if (deck.getCards() == null) return List.of();
        return deck.getCards().stream()
                .filter(c -> c.getCardCategory() == category)
                .toList();
    }
}
