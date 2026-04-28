package dm.dracolich.deckBuilder.core.mapper;

import dm.dracolich.deckBuilder.data.entity.DeckCardEntity;
import dm.dracolich.deckBuilder.dto.DeckCardDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.FIELD)
public interface DeckCardMapper {
    DeckCardEntity toEntity(DeckCardDto dto);
    DeckCardDto toDto(DeckCardEntity deckCardEntity);
}
