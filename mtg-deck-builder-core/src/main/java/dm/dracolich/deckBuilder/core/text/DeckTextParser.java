package dm.dracolich.deckBuilder.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeckTextParser {

    /**
     * Matches: "1 Sol Ring" or "4 Lightning Bolt (M21) 162" or "1 Sol Ring *F*"
     * Group 1: count
     * Group 2: name (everything until set parens, asterisk metadata, or end of line)
     */
    private static final Pattern CARD_LINE = Pattern.compile(
            "^\\s*(\\d+)\\s+([^*(]+?)(?:\\s*\\(.+|\\s*\\*.+)?\\s*$");

    private DeckTextParser() {
    }

    public static List<ParsedLine> parse(String text) {
        List<ParsedLine> result = new ArrayList<>();
        if (text == null) return result;

        ParsedLine.Section currentSection = ParsedLine.Section.MAINBOARD;
        String[] lines = text.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String raw = lines[i];
            String trimmed = raw.trim();

            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("//")) {
                ParsedLine.Section detected = detectSection(trimmed);
                if (detected != null) currentSection = detected;
                continue;
            }

            Matcher m = CARD_LINE.matcher(trimmed);
            if (!m.matches()) {
                result.add(new ParsedLine(
                        lineNumber, raw, null, null, currentSection,
                        "Could not parse line — expected '<count> <card name>'"));
                continue;
            }

            int count;
            try {
                count = Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                result.add(new ParsedLine(
                        lineNumber, raw, null, null, currentSection,
                        "Invalid count"));
                continue;
            }

            String name = m.group(2).trim();
            if (name.isEmpty()) {
                result.add(new ParsedLine(
                        lineNumber, raw, count, null, currentSection,
                        "Missing card name"));
                continue;
            }
            if (count < 1) {
                result.add(new ParsedLine(
                        lineNumber, raw, count, name, currentSection,
                        "Count must be at least 1"));
                continue;
            }

            result.add(new ParsedLine(lineNumber, raw, count, name, currentSection, null));
        }

        return result;
    }

    private static ParsedLine.Section detectSection(String marker) {
        String lower = marker.toLowerCase();
        if (lower.contains("commander")) return ParsedLine.Section.COMMANDER;
        if (lower.contains("sideboard") || lower.contains("outside")) return ParsedLine.Section.SIDEBOARD;
        if (lower.contains("maybe")) return ParsedLine.Section.MAYBEBOARD;
        return null;
    }
}
