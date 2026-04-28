package dm.dracolich.deckBuilder.core.validation;

import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.dto.ValidationViolationDto;
import dm.dracolich.mtgLibrary.dto.enums.Format;

import java.util.List;

public interface FormatRuleSet {
    Format format();
    List<ValidationViolationDto> validate(DeckEntity deck);
}
