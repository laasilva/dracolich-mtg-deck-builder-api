package dm.dracolich.deckBuilder.core.mapper;

import dm.dracolich.deckBuilder.data.entity.DeckCardEntity;
import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.dto.DeckDto;
import dm.dracolich.deckBuilder.dto.enums.CardCategory;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.FIELD,
            uses = DeckCardMapper.class)
public interface DeckMapper {
    DeckDto toDto(DeckEntity entity);
    DeckEntity toEntity(DeckDto dto);

    @AfterMapping
    default void computeCounts(@MappingTarget DeckDto dto, DeckEntity entity) {
        int commanderCount = sumCount(entity.getCommander());
        int mainboard = sumByCategory(entity.getCards(), CardCategory.MAINBOARD);
        int sideboard = sumByCategory(entity.getCards(), CardCategory.SIDEBOARD);
        int maybeboard = sumByCategory(entity.getCards(), CardCategory.MAYBE_BOARD);

        dto.setMainboardCount(mainboard);
        dto.setSideboardCount(sideboard);
        dto.setMaybeboardCount(maybeboard);
        dto.setCardCount(commanderCount + mainboard + sideboard + maybeboard);
    }

    private static int sumCount(List<DeckCardEntity> cards) {
        if (cards == null) return 0;
        return cards.stream()
                .mapToInt(c -> c.getCount() != null ? c.getCount() : 1)
                .sum();
    }

    private static int sumByCategory(List<DeckCardEntity> cards, CardCategory category) {
        if (cards == null) return 0;
        return cards.stream()
                .filter(c -> c.getCardCategory() == category)
                .mapToInt(c -> c.getCount() != null ? c.getCount() : 1)
                .sum();
    }
}
