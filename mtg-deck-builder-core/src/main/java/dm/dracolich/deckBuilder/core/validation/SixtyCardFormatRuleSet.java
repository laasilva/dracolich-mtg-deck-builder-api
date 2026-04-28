package dm.dracolich.deckBuilder.core.validation;

import dm.dracolich.deckBuilder.core.stats.DeckStatsCalculator;
import dm.dracolich.deckBuilder.data.entity.DeckCardEntity;
import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.dto.ValidationViolationDto;
import dm.dracolich.deckBuilder.dto.enums.CardCategory;
import dm.dracolich.deckBuilder.dto.enums.ValidationSeverity;
import dm.dracolich.mtgLibrary.dto.enums.Format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SixtyCardFormatRuleSet implements FormatRuleSet {

    private static final int MIN_MAINBOARD = 60;
    private static final int MAX_SIDEBOARD = 15;
    private static final int MAX_COPIES = 4;

    private final Format format;

    public SixtyCardFormatRuleSet(Format format) {
        this.format = format;
    }

    @Override
    public Format format() {
        return format;
    }

    @Override
    public List<ValidationViolationDto> validate(DeckEntity deck) {
        List<ValidationViolationDto> violations = new ArrayList<>();

        if (deck.getCommander() != null && !deck.getCommander().isEmpty()) {
            violations.add(new ValidationViolationDto(
                    "no_commander",
                    format + " decks do not have a commander",
                    null,
                    ValidationSeverity.ERROR));
        }

        validateMainboardSize(deck, violations);
        validateSideboardSize(deck, violations);
        validateFourOf(deck, violations);

        return violations;
    }

    private void validateMainboardSize(DeckEntity deck, List<ValidationViolationDto> violations) {
        int mainboard = countByCategory(deck, CardCategory.MAINBOARD);
        if (mainboard < MIN_MAINBOARD) {
            violations.add(new ValidationViolationDto(
                    "mainboard_size",
                    "Mainboard must have at least " + MIN_MAINBOARD + " cards (got " + mainboard + ")",
                    null,
                    ValidationSeverity.ERROR));
        }
    }

    private void validateSideboardSize(DeckEntity deck, List<ValidationViolationDto> violations) {
        int sideboard = countByCategory(deck, CardCategory.SIDEBOARD);
        if (sideboard > MAX_SIDEBOARD) {
            violations.add(new ValidationViolationDto(
                    "sideboard_size",
                    "Sideboard exceeds " + MAX_SIDEBOARD + " cards (got " + sideboard + ")",
                    null,
                    ValidationSeverity.ERROR));
        }
    }

    private void validateFourOf(DeckEntity deck, List<ValidationViolationDto> violations) {
        if (deck.getCards() == null) return;
        Map<String, Integer> totals = new HashMap<>();
        for (DeckCardEntity card : deck.getCards()) {
            if (card.getCardCategory() == CardCategory.MAYBE_BOARD) continue;
            int count = card.getCount() != null ? card.getCount() : 1;
            totals.merge(card.getName(), count, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            if (e.getValue() <= MAX_COPIES) continue;
            DeckCardEntity sample = sampleByName(deck, e.getKey());
            if (sample != null && DeckStatsCalculator.isBasicLand(sample)) continue;
            violations.add(new ValidationViolationDto(
                    "four_of",
                    e.getKey() + " appears " + e.getValue() + " times — max " + MAX_COPIES + " of any non-basic card",
                    e.getKey(),
                    ValidationSeverity.ERROR));
        }
    }

    private DeckCardEntity sampleByName(DeckEntity deck, String name) {
        if (deck.getCards() == null) return null;
        return deck.getCards().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElse(null);
    }

    private int countByCategory(DeckEntity deck, CardCategory category) {
        if (deck.getCards() == null) return 0;
        int total = 0;
        for (DeckCardEntity card : deck.getCards()) {
            if (card.getCardCategory() != category) continue;
            total += card.getCount() != null ? card.getCount() : 1;
        }
        return total;
    }
}
