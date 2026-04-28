package dm.dracolich.deckBuilder.core.validation;

import dm.dracolich.deckBuilder.core.stats.DeckStatsCalculator;
import dm.dracolich.deckBuilder.data.entity.DeckCardEntity;
import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.dto.ValidationViolationDto;
import dm.dracolich.deckBuilder.dto.enums.ValidationSeverity;
import dm.dracolich.mtgLibrary.dto.enums.Color;
import dm.dracolich.mtgLibrary.dto.enums.Format;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CommanderRuleSet implements FormatRuleSet {

    private static final int DECK_SIZE = 100;

    @Override
    public Format format() {
        return Format.COMMANDER;
    }

    @Override
    public List<ValidationViolationDto> validate(DeckEntity deck) {
        List<ValidationViolationDto> violations = new ArrayList<>();

        validateCommanderPresence(deck, violations);
        validateDeckSize(deck, violations);
        validateSingleton(deck, violations);
        validateColorIdentity(deck, violations);

        return violations;
    }

    private void validateCommanderPresence(DeckEntity deck, List<ValidationViolationDto> violations) {
        if (deck.getCommander() == null || deck.getCommander().isEmpty()) {
            violations.add(new ValidationViolationDto(
                    "commander_required",
                    "Commander deck must have a commander",
                    null,
                    ValidationSeverity.ERROR));
            return;
        }
        if (deck.getCommander().size() > 2) {
            violations.add(new ValidationViolationDto(
                    "commander_count",
                    "Commander deck can have at most 2 commanders (Partner / Background)",
                    null,
                    ValidationSeverity.ERROR));
        }
    }

    private void validateDeckSize(DeckEntity deck, List<ValidationViolationDto> violations) {
        int total = totalCardCount(deck);
        if (total != DECK_SIZE) {
            int commanderCount = deck.getCommander() == null ? 0
                    : deck.getCommander().stream()
                            .mapToInt(c -> c.getCount() != null ? c.getCount() : 1)
                            .sum();
            int mainCount = total - commanderCount;
            violations.add(new ValidationViolationDto(
                    "deck_size",
                    "Commander decks must have exactly " + DECK_SIZE + " cards including the commander " +
                            "(got " + total + ": " + commanderCount + " commander + " + mainCount + " main)",
                    null,
                    ValidationSeverity.ERROR));
        }
    }

    private void validateSingleton(DeckEntity deck, List<ValidationViolationDto> violations) {
        if (deck.getCards() == null) return;
        for (DeckCardEntity card : deck.getCards()) {
            int count = card.getCount() != null ? card.getCount() : 1;
            if (count > 1 && !DeckStatsCalculator.isBasicLand(card)) {
                violations.add(new ValidationViolationDto(
                        "singleton",
                        card.getName() + " appears " + count + " times — Commander is a singleton format",
                        card.getName(),
                        ValidationSeverity.ERROR));
            }
        }
    }

    private void validateColorIdentity(DeckEntity deck, List<ValidationViolationDto> violations) {
        if (deck.getCommander() == null || deck.getCommander().isEmpty()) return;
        if (deck.getCards() == null) return;

        Set<Color> identity = EnumSet.noneOf(Color.class);
        for (DeckCardEntity c : deck.getCommander()) {
            if (c.getColors() != null) identity.addAll(c.getColors());
        }

        for (DeckCardEntity card : deck.getCards()) {
            if (card.getColors() == null || card.getColors().isEmpty()) continue;
            for (Color color : card.getColors()) {
                if (!identity.contains(color)) {
                    violations.add(new ValidationViolationDto(
                            "color_identity",
                            card.getName() + " has color " + color + " outside commander's identity " + identity,
                            card.getName(),
                            ValidationSeverity.ERROR));
                    break;
                }
            }
        }
    }

    private int totalCardCount(DeckEntity deck) {
        int total = 0;
        if (deck.getCommander() != null) {
            for (DeckCardEntity c : deck.getCommander()) {
                total += c.getCount() != null ? c.getCount() : 1;
            }
        }
        if (deck.getCards() != null) {
            for (DeckCardEntity c : deck.getCards()) {
                total += c.getCount() != null ? c.getCount() : 1;
            }
        }
        return total;
    }
}
