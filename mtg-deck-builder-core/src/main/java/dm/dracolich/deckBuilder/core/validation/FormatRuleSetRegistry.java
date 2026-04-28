package dm.dracolich.deckBuilder.core.validation;

import dm.dracolich.mtgLibrary.dto.enums.Format;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Component
public class FormatRuleSetRegistry {

    private final Map<Format, FormatRuleSet> rules;

    public FormatRuleSetRegistry() {
        rules = new EnumMap<>(Format.class);
        rules.put(Format.COMMANDER, new CommanderRuleSet());
        rules.put(Format.STANDARD, new SixtyCardFormatRuleSet(Format.STANDARD));
        rules.put(Format.MODERN, new SixtyCardFormatRuleSet(Format.MODERN));
        rules.put(Format.PIONEER, new SixtyCardFormatRuleSet(Format.PIONEER));
        rules.put(Format.PAUPER, new SixtyCardFormatRuleSet(Format.PAUPER));
    }

    public Optional<FormatRuleSet> get(Format format) {
        return Optional.ofNullable(rules.get(format));
    }
}
