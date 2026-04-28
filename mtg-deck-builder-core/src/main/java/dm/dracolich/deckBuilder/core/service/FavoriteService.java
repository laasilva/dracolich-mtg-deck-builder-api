package dm.dracolich.deckBuilder.core.service;

import reactor.core.publisher.Mono;

public interface FavoriteService {
    Mono<Boolean> favoriteDeck(String deckId);
    Mono<Boolean> unfavoriteDeck(String deckId);
}
