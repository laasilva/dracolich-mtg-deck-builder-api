package dm.dracolich.deckBuilder.data.repository.custom;

import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.dto.enums.DeckStatus;
import dm.dracolich.deckBuilder.dto.enums.Visibility;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Mono;

public interface DeckCustomRepository {
    Mono<Page<DeckEntity>> findUserDecksByFilters(String userId,
                                                  Format format,
                                                  DeckStatus status,
                                                  Visibility visibility,
                                                  int page,
                                                  int size);

    Mono<Page<DeckEntity>> findPopularPublicDecks(Format format, int page, int size);

    Mono<Page<DeckEntity>> findLatestPublicDecks(Format format, int page, int size);
}
