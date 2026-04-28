package dm.dracolich.deckBuilder.core.service;

import dm.dracolich.deckBuilder.dto.CardRequest;
import dm.dracolich.deckBuilder.dto.CreateDeckRequest;
import dm.dracolich.deckBuilder.dto.DeckAnalysisDto;
import dm.dracolich.deckBuilder.dto.DeckDto;
import dm.dracolich.deckBuilder.dto.DeckStatsDto;
import dm.dracolich.deckBuilder.dto.DeckValidationDto;
import dm.dracolich.deckBuilder.dto.ImportDeckRequest;
import dm.dracolich.deckBuilder.dto.ImportValidateRequest;
import dm.dracolich.deckBuilder.dto.ValidateDeckRequest;
import dm.dracolich.deckBuilder.dto.enums.DeckStatus;
import dm.dracolich.deckBuilder.dto.enums.Visibility;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Mono;

public interface DeckService {
    Mono<DeckDto> createDeck(CreateDeckRequest request);
    Mono<DeckDto> addCardToDeck(String id, CardRequest request);
    Mono<DeckDto> copyDeck(String id);
    Mono<DeckDto> fetchDeckById(String id);
    Mono<Boolean> deleteDeckById(String id);
    Mono<Boolean> removeCardFromDeck(String id, String cardId);
    Mono<Page<DeckDto>> listUserDecks(Format format, DeckStatus status, Visibility visibility, int page, int size);
    Mono<Page<DeckDto>> listPopularDecks(Format format, int page, int size);
    Mono<Page<DeckDto>> listLatestDecks(Format format, int page, int size);
    Mono<DeckStatsDto> fetchDeckStats(String id);
    Mono<DeckValidationDto> fetchDeckValidation(String id);
    Mono<DeckAnalysisDto> validateUnsavedDeck(ValidateDeckRequest request);
    Mono<DeckDto> importDeck(ImportDeckRequest request);
    Mono<String> exportDeck(String id);
    Mono<DeckAnalysisDto> validateImportedDeck(ImportValidateRequest request);
}
