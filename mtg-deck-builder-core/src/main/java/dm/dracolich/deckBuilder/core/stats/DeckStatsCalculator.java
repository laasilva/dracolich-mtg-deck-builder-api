package dm.dracolich.deckBuilder.core.stats;

import dm.dracolich.deckBuilder.data.entity.DeckCardEntity;
import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.dto.DeckStatsDto;
import dm.dracolich.mtgLibrary.dto.enums.Color;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class DeckStatsCalculator {

    private static final Set<String> BASIC_LAND_NAMES = Set.of(
            "Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes",
            "Snow-Covered Plains", "Snow-Covered Island", "Snow-Covered Swamp",
            "Snow-Covered Mountain", "Snow-Covered Forest", "Snow-Covered Wastes");

    private static final Set<String> CARD_TYPES = Set.of(
            "Creature", "Planeswalker", "Battle", "Land",
            "Instant", "Sorcery", "Artifact", "Enchantment", "Tribal");

    private DeckStatsCalculator() {
    }

    public static DeckStatsDto compute(DeckEntity deck) {
        List<DeckCardEntity> all = allCards(deck);

        int cardCount = totalCount(all);
        int landCount = countWhere(all, DeckStatsCalculator::isLand);
        Map<Integer, Integer> manaCurve = manaCurve(all);
        Map<Color, Double> colorPie = colorPie(all);
        Map<String, Integer> typeBreakdown = typeBreakdown(all);
        Double averageCmc = averageCmc(all);
        List<String> warnings = warnings(deck, cardCount, landCount, averageCmc);

        return new DeckStatsDto(
                deck.getId(),
                cardCount,
                landCount,
                manaCurve,
                colorPie,
                typeBreakdown,
                averageCmc,
                warnings);
    }

    private static List<DeckCardEntity> allCards(DeckEntity deck) {
        List<DeckCardEntity> all = new ArrayList<>();
        if (deck.getCommander() != null) all.addAll(deck.getCommander());
        if (deck.getCards() != null) all.addAll(deck.getCards());
        return all;
    }

    private static int totalCount(List<DeckCardEntity> cards) {
        return cards.stream()
                .mapToInt(c -> c.getCount() != null ? c.getCount() : 1)
                .sum();
    }

    private static int countWhere(List<DeckCardEntity> cards, java.util.function.Predicate<DeckCardEntity> filter) {
        return cards.stream()
                .filter(filter)
                .mapToInt(c -> c.getCount() != null ? c.getCount() : 1)
                .sum();
    }

    public static boolean isLand(DeckCardEntity card) {
        return cardTypes(card).contains("Land");
    }

    public static boolean isBasicLand(DeckCardEntity card) {
        return card.getName() != null && BASIC_LAND_NAMES.contains(card.getName());
    }

    private static Set<String> cardTypes(DeckCardEntity card) {
        if (card.getTypeLine() == null) return Set.of();
        String left = card.getTypeLine().split("—", 2)[0];
        return Stream.of(left.split("\\s+"))
                .filter(CARD_TYPES::contains)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static Map<Integer, Integer> manaCurve(List<DeckCardEntity> cards) {
        Map<Integer, Integer> curve = new TreeMap<>();
        for (DeckCardEntity c : cards) {
            if (isLand(c) || c.getCmc() == null) continue;
            int bucket = c.getCmc().intValue();
            int qty = c.getCount() != null ? c.getCount() : 1;
            curve.merge(bucket, qty, Integer::sum);
        }
        return curve;
    }

    private static Map<Color, Double> colorPie(List<DeckCardEntity> cards) {
        Map<Color, Integer> pips = new EnumMap<>(Color.class);
        int total = 0;
        for (DeckCardEntity c : cards) {
            if (isLand(c) || c.getColors() == null || c.getColors().isEmpty()) continue;
            int qty = c.getCount() != null ? c.getCount() : 1;
            for (Color color : c.getColors()) {
                pips.merge(color, qty, Integer::sum);
                total += qty;
            }
        }
        if (total == 0) return Map.of();
        Map<Color, Double> pie = new EnumMap<>(Color.class);
        for (Map.Entry<Color, Integer> e : pips.entrySet()) {
            pie.put(e.getKey(), Math.round(e.getValue() * 1000.0 / total) / 1000.0);
        }
        return pie;
    }

    private static Map<String, Integer> typeBreakdown(List<DeckCardEntity> cards) {
        Map<String, Integer> breakdown = new HashMap<>();
        for (DeckCardEntity c : cards) {
            int qty = c.getCount() != null ? c.getCount() : 1;
            for (String type : cardTypes(c)) {
                breakdown.merge(type, qty, Integer::sum);
            }
        }
        return breakdown;
    }

    private static Double averageCmc(List<DeckCardEntity> cards) {
        double sum = 0;
        int count = 0;
        for (DeckCardEntity c : cards) {
            if (isLand(c) || c.getCmc() == null) continue;
            int qty = c.getCount() != null ? c.getCount() : 1;
            sum += c.getCmc() * qty;
            count += qty;
        }
        if (count == 0) return 0.0;
        return Math.round((sum / count) * 100.0) / 100.0;
    }

    private static List<String> warnings(DeckEntity deck, int cardCount, int landCount, Double averageCmc) {
        List<String> warnings = new ArrayList<>();
        if (cardCount > 0 && landCount > 0) {
            double landRatio = (double) landCount / cardCount;
            if (landRatio < 0.30) {
                warnings.add("Low land count (" + landCount + "/" + cardCount + ") — most decks run 35-40%");
            }
            if (landRatio > 0.50) {
                warnings.add("High land count (" + landCount + "/" + cardCount + ") — most decks run 35-40%");
            }
        }
        if (averageCmc != null && averageCmc > 4.5) {
            warnings.add("High average mana value (" + averageCmc + ") — deck may be slow");
        }
        return warnings;
    }
}
