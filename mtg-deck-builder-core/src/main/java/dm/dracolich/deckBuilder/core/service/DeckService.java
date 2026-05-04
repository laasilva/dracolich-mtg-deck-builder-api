package dm.dracolich.deckBuilder.core.service;

import dm.dracolich.deckBuilder.dto.CardRequest;
import dm.dracolich.deckBuilder.dto.CreateDeckRequest;
import dm.dracolich.deckBuilder.dto.DeckAnalysisDto;
import dm.dracolich.deckBuilder.dto.DeckDto;
import dm.dracolich.deckBuilder.dto.DeckStatsDto;
import dm.dracolich.deckBuilder.dto.DeckValidationDto;
import dm.dracolich.deckBuilder.dto.ImportDeckRequest;
import dm.dracolich.deckBuilder.dto.ImportValidateRequest;
import dm.dracolich.deckBuilder.dto.UpdateCardRequest;
import dm.dracolich.deckBuilder.dto.UpdateDeckRequest;
import dm.dracolich.deckBuilder.dto.ValidateDeckRequest;
import dm.dracolich.deckBuilder.dto.enums.CardCategory;
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
    Mono<DeckDto> updateDeck(String id, UpdateDeckRequest request);
    Mono<DeckDto> updateCardInDeck(String id, String cardId, CardCategory category, UpdateCardRequest request);
    Mono<DeckDto> claimDeck(String id, String cookieAnonId);
    Mono<Page<DeckDto>> listUserDecks(Format format, DeckStatus status, Visibility visibility, int page, int size);
    Mono<Page<DeckDto>> listPopularDecks(Format format, int page, int size);
    Mono<Page<DeckDto>> listLatestDecks(Format format, int page, int size);
    Mono<Page<DeckDto>> listPublicUserDecks(String userId, Format format, int page, int size);
    Mono<Page<DeckDto>> listPublicUserFavorites(String userId, int page, int size);
    Mono<DeckStatsDto> fetchDeckStats(String id);
    Mono<DeckValidationDto> fetchDeckValidation(String id);
    Mono<DeckAnalysisDto> validateUnsavedDeck(ValidateDeckRequest request);
    Mono<DeckDto> importDeck(ImportDeckRequest request);
    Mono<String> exportDeck(String id);
    Mono<DeckAnalysisDto> validateImportedDeck(ImportValidateRequest request);
}
