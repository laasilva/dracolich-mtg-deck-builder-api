package dm.dracolich.deckBuilder.core.service;

import dm.dracolich.deckBuilder.dto.DeckAiResultDto;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeckAiService {
    Mono<DeckAiResultDto> suggestForDeck(String deckId);
    Mono<DeckAiResultDto> analyzeDeck(String deckId);
    Flux<String> suggestForCard(String cardId, Format format);
}
