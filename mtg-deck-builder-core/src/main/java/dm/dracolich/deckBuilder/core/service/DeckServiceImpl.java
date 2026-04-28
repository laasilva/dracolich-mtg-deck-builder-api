package dm.dracolich.deckBuilder.core.service;

import dm.dracolich.deckBuilder.core.helpers.ErrorUtil;
import dm.dracolich.deckBuilder.core.mapper.DeckMapper;
import dm.dracolich.deckBuilder.core.stats.DeckStatsCalculator;
import dm.dracolich.deckBuilder.core.text.DeckTextExporter;
import dm.dracolich.deckBuilder.core.text.DeckTextParser;
import dm.dracolich.deckBuilder.core.text.ParsedLine;
import dm.dracolich.deckBuilder.core.validation.FormatRuleSet;
import dm.dracolich.deckBuilder.core.validation.FormatRuleSetRegistry;
import dm.dracolich.deckBuilder.data.entity.AuditEntity;
import dm.dracolich.deckBuilder.data.entity.DeckCardEntity;
import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.data.repository.DeckRepository;
import dm.dracolich.deckBuilder.dto.CardRequest;
import dm.dracolich.deckBuilder.dto.CreateDeckRequest;
import dm.dracolich.deckBuilder.dto.DeckAnalysisDto;
import dm.dracolich.deckBuilder.dto.DeckDto;
import dm.dracolich.deckBuilder.dto.DeckStatsDto;
import dm.dracolich.deckBuilder.dto.DeckValidationDto;
import dm.dracolich.deckBuilder.dto.ImportDeckRequest;
import dm.dracolich.deckBuilder.dto.ImportValidateRequest;
import dm.dracolich.deckBuilder.dto.ValidateDeckRequest;
import dm.dracolich.deckBuilder.dto.ValidationViolationDto;
import dm.dracolich.deckBuilder.dto.enums.CardCategory;
import dm.dracolich.deckBuilder.dto.enums.DeckStatus;
import dm.dracolich.deckBuilder.dto.enums.ValidationSeverity;
import dm.dracolich.deckBuilder.dto.enums.Visibility;
import dm.dracolich.deckBuilder.integration.DracolichMtgLibraryClient;
import dm.dracolich.forge.exception.ResponseException;
import dm.dracolich.forge.security.OwnershipResolver;
import dm.dracolich.forge.security.Principal;
import dm.dracolich.forge.security.ReactiveSecurityContextUtil;
import dm.dracolich.mtgLibrary.dto.CardDto;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dm.dracolich.deckBuilder.core.helpers.ErrorUtil.importFailed;
import static dm.dracolich.deckBuilder.core.helpers.ErrorUtil.notFound;
import static dm.dracolich.deckBuilder.core.helpers.ErrorUtil.unauthorized;

@Service
@RequiredArgsConstructor
public class DeckServiceImpl implements DeckService {
    private final DeckRepository repo;
    private final DeckMapper mapper;

    private final DracolichMtgLibraryClient libraryFeignClient;
    private final FormatRuleSetRegistry ruleRegistry;

    @Override
    public Mono<DeckDto> createDeck(CreateDeckRequest request) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .flatMap(principal -> buildCreateDeckEntity(principal, request))
                .flatMap(repo::save)
            .map(mapper::toDto);
    }

    @Override
    public Mono<DeckDto> addCardToDeck(String id, CardRequest request) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .flatMap(principal -> buildAddCardToDeckEntity(principal, id, request))
                .flatMap(repo::save)
            .map(mapper::toDto);
    }

    @Override
    public Mono<DeckDto> copyDeck(String id) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .flatMap(principal -> buildCopyDeckEntity(principal, id))
                .flatMap(repo::save)
            .map(mapper::toDto);
    }

    @Override
    public Mono<DeckDto> fetchDeckById(String id) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .flatMap(principal -> buildFetchDeckByIdAndUserId(principal, id))
                .map(mapper::toDto);
    }

    @Override
    public Mono<Boolean> deleteDeckById(String id) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .map(principal -> buildDeleteDeckEntity(principal, id))
            .thenReturn(true);
    }

    @Override
    public Mono<Boolean> removeCardFromDeck(String id, String cardId) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .flatMap(principal -> buildRemoveCardFromDeck(principal, id, cardId))
                .flatMap(repo::save)
            .thenReturn(true);
    }

    @Override
    public Mono<Page<DeckDto>> listUserDecks(Format format, DeckStatus status, Visibility visibility, int page, int size) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .flatMap(principal -> buildListUserDecks(principal, format, status, visibility, page, size));
    }

    private Mono<Page<DeckDto>> buildListUserDecks(Principal principal, Format format, DeckStatus status, Visibility visibility, int page, int size) {
        if (!principal.isUser()) {
            return Mono.error(unauthorized());
        }
        return repo.findUserDecksByFilters(principal.id(), format, status, visibility, page, size)
                .map(p -> p.map(mapper::toDto));
    }

    @Override
    public Mono<Page<DeckDto>> listPopularDecks(Format format, int page, int size) {
        return repo.findPopularPublicDecks(format, page, size)
                .map(p -> p.map(mapper::toDto));
    }

    @Override
    public Mono<Page<DeckDto>> listLatestDecks(Format format, int page, int size) {
        return repo.findLatestPublicDecks(format, page, size)
                .map(p -> p.map(mapper::toDto));
    }

    @Override
    public Mono<DeckStatsDto> fetchDeckStats(String id) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .flatMap(principal -> loadAccessibleDeck(id, principal, "fetch_deck_stats"))
                .map(DeckStatsCalculator::compute);
    }

    @Override
    public Mono<DeckValidationDto> fetchDeckValidation(String id) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .flatMap(principal -> loadAccessibleDeck(id, principal, "fetch_deck_validation"))
                .map(this::validateDeck);
    }

    @Override
    public Mono<DeckAnalysisDto> validateUnsavedDeck(ValidateDeckRequest request) {
        return buildTransientDeck(request)
                .map(deck -> new DeckAnalysisDto(validateDeck(deck), DeckStatsCalculator.compute(deck)));
    }

    private Mono<DeckEntity> loadAccessibleDeck(String id, Principal principal, String context) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(notFound(context, "id", id)))
                .filter(deck -> canAccess(deck, principal))
                .switchIfEmpty(Mono.error(unauthorized()));
    }

    private DeckValidationDto validateDeck(DeckEntity deck) {
        Format format = deck.getFormat();
        if (format == null) {
            return new DeckValidationDto(deck.getId(), null, false, List.of(
                    new ValidationViolationDto("format_required",
                            "Deck has no format set",
                            null,
                            ValidationSeverity.ERROR)));
        }
        Optional<FormatRuleSet> rules = ruleRegistry.get(format);
        if (rules.isEmpty()) {
            return new DeckValidationDto(deck.getId(), format, false, List.of(
                    new ValidationViolationDto("format_unsupported",
                            format + " is not yet supported for validation",
                            null,
                            ValidationSeverity.ERROR)));
        }
        List<ValidationViolationDto> violations = rules.get().validate(deck);
        boolean valid = violations.stream()
                .noneMatch(v -> v.severity() == ValidationSeverity.ERROR);
        return new DeckValidationDto(deck.getId(), format, valid, violations);
    }

    private Mono<DeckEntity> buildTransientDeck(ValidateDeckRequest request) {
        Mono<List<DeckCardEntity>> commanderMono = request.commander() != null
                ? buildDeckCard(request.commander()).map(List::of)
                : Mono.just(List.of());

        Mono<List<DeckCardEntity>> cardsMono = Flux.fromIterable(request.cards())
                .flatMap(this::buildDeckCard)
                .collectList();

        return Mono.zip(commanderMono, cardsMono)
                .map(tuple -> DeckEntity.builder()
                        .format(request.format())
                        .commander(new ArrayList<>(tuple.getT1()))
                        .cards(new ArrayList<>(tuple.getT2()))
                        .build());
    }

    private Mono<DeckEntity> buildCopyDeckEntity(Principal principal, String id) {
        if(principal == null || principal.isAnon()) {
            return Mono.error(unauthorized());
        }
        Instant now = Instant.now();

        return repo.findById(id)
                .switchIfEmpty(Mono.error(notFound("copy_deck", "deck", id)))
                .filter(deck -> deck.getVisibility().equals(Visibility.PUBLIC))
                .switchIfEmpty(Mono.error(unauthorized()))
                .map(source -> DeckEntity.builder()
                        .copiedFromId(source.getId())
                        .userId(principal.id())
                        .audit(AuditEntity.builder()
                                .createdAt(now)
                                .lastModified(now)
                                .build())
                        .name(source.getName())
                        .description(source.getDescription())
                        .colors(source.getColors())
                        .format(source.getFormat())
                        .status(source.getStatus())
                        .commander(source.getCommander())
                        .cards(source.getCards())
                        .visibility(Visibility.PRIVATE)
                        .favoritesCount(0L)
                    .build());
    }

    private Mono<DeckEntity> buildFetchDeckByIdAndUserId(Principal principal, String id) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(notFound("fetch_deck_by_id", "id", id)))
                .filter(deck -> canAccess(deck, principal))
                .switchIfEmpty(Mono.error(unauthorized()));
    }

    private Mono<DeckEntity> buildCreateDeckEntity(Principal principal, CreateDeckRequest request) {
        Mono<List<DeckCardEntity>> commanderMono = request.commander() != null ?
                buildDeckCard(request.commander()).map(List::of) : Mono.just(List.of());

        Mono<List<DeckCardEntity>> cardsMono = Flux.fromIterable(request.cards())
                        .flatMap(this::buildDeckCard).collectList();

        Instant now = Instant.now();

        return Mono.zip(commanderMono, cardsMono)
                .map(touple -> {
                    var builder = DeckEntity.builder()
                            .copiedFromId(request.copiedFromId())
                            .audit(AuditEntity.builder()
                                    .createdAt(now)
                                    .lastModified(now)
                                    .build())
                            .name(request.name())
                            .description(request.description())
                            .format(request.format())
                            .status(request.deckStatus() != null ? request.deckStatus() : DeckStatus.DRAFT)
                            .commander(touple.getT1())
                            .cards(touple.getT2())
                            .visibility(request.visibility() != null ? request.visibility() : Visibility.PRIVATE);

                    return switch (principal.type()) {
                        case USER -> builder.userId(principal.id()).build();
                        case ANON -> builder.anonId(principal.id()).visibility(Visibility.PRIVATE).build();
                    };
                });
    }

    private Mono<DeckCardEntity> buildDeckCard(CardRequest card) {
        return libraryFeignClient.fetchCardById(card.cardId())
                .switchIfEmpty(Mono.error(notFound("create_deck", "card", card.cardId())))
                .map(libraryCard -> snapshot(libraryCard, card.count(), card.cardCategory()));
    }

    private DeckCardEntity snapshot(CardDto libraryCard, Integer count, CardCategory category) {
        return DeckCardEntity.builder()
                .cardId(libraryCard.getId())
                .name(libraryCard.getName())
                .manaCost(libraryCard.getDefaultFace().getGameplayProperty().getManaCost())
                .cmc(libraryCard.getDefaultFace().getGameplayProperty().getManaValue())
                .colors(libraryCard.getDefaultFace().getGameplayProperty().getColors())
                .typeLine(libraryCard.getDefaultFace().getFullType())
                .imageUri(libraryCard.getDefaultArt() != null
                        ? libraryCard.getDefaultArt().getImageUris() : null)
                .count(count)
                .cardCategory(category)
                .build();
    }

    private Mono<DeckEntity> buildAddCardToDeckEntity(Principal principal, String id, CardRequest request) {
        return loadOwnedDeckOrFail(id, principal, "add_card_to_deck")
                .flatMap(deckEntity -> buildDeckCard(request)
                        .map(card -> {
                            deckEntity.getCards().add(card);
                            return touchLastModified(deckEntity);
                        }));
    }

    private Mono<Boolean> buildDeleteDeckEntity(Principal principal, String id) {
        return loadOwnedDeckOrFail(id, principal, "delete_deck")
                .flatMap(deck -> {
                    if (!principal.isUser()) {
                        return Mono.error(unauthorized());
                    }
                    return repo.delete(deck);
                })
                .thenReturn(true);
    }

    private Mono<DeckEntity> buildRemoveCardFromDeck(Principal principal, String id, String cardId) {
        return loadOwnedDeckOrFail(id, principal, "remove_card_from_deck")
                .map(deckEntity -> {
                    deckEntity.getCards().removeIf(card -> card.getCardId().equals(cardId));
                    return touchLastModified(deckEntity);
                });
    }

    private Mono<DeckEntity> loadOwnedDeckOrFail(String id, Principal principal, String context) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(notFound(context, "deck", id)))
                .flatMap(deck -> OwnershipResolver.ownedBy(principal, deck.getUserId(), deck.getAnonId())
                        ? Mono.just(deck)
                        : Mono.error(unauthorized()));
    }

    /**
     * Bumps audit.lastModified on the deck. Call from any path that mutates the deck
     * so AI cache invalidation and downstream sync operations can detect changes.
     */
    private static DeckEntity touchLastModified(DeckEntity deck) {
        if (deck.getAudit() == null) {
            deck.setAudit(AuditEntity.builder().createdAt(Instant.now()).build());
        }
        deck.getAudit().setLastModified(Instant.now());
        return deck;
    }

    private boolean canAccess(DeckEntity deck, Principal principal) {
        if(deck.getVisibility() == Visibility.PUBLIC)
            return true;

        if(principal == null)
            return false;

        if(principal.isUser())
            return principal.id().equals(deck.getUserId());

        if(principal.isAnon())
            return principal.id().equals(deck.getAnonId());

        return false;
    }

    @Override
    public Mono<DeckDto> importDeck(ImportDeckRequest request) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .flatMap(principal -> resolveImport(request.deckText())
                        .map(resolved -> assembleImportedDeck(principal, request, resolved)))
                .flatMap(repo::save)
                .map(mapper::toDto);
    }

    @Override
    public Mono<String> exportDeck(String id) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .flatMap(principal -> loadAccessibleDeck(id, principal, "export_deck"))
                .map(DeckTextExporter::export);
    }

    @Override
    public Mono<DeckAnalysisDto> validateImportedDeck(ImportValidateRequest request) {
        return resolveImport(request.deckText())
                .map(resolved -> {
                    DeckEntity transientDeck = DeckEntity.builder()
                            .format(request.format())
                            .commander(resolved.commander())
                            .cards(resolved.cards())
                            .build();
                    return new DeckAnalysisDto(
                            validateDeck(transientDeck),
                            DeckStatsCalculator.compute(transientDeck));
                });
    }

    private record ResolvedImport(List<DeckCardEntity> commander, List<DeckCardEntity> cards) {}

    private Mono<ResolvedImport> resolveImport(String deckText) {
        List<ParsedLine> parsed = DeckTextParser.parse(deckText);

        List<ErrorUtil.ImportFailure> parseFailures = parsed.stream()
                .filter(p -> !p.isValid())
                .map(p -> new ErrorUtil.ImportFailure(p.lineNumber(), p.originalLine(), p.parseError()))
                .toList();

        if (parsed.stream().filter(ParsedLine::isValid).findAny().isEmpty() && parseFailures.isEmpty()) {
            return Mono.error(importFailed(List.of(new ErrorUtil.ImportFailure(0, "", "Deck text is empty"))));
        }

        return Flux.fromIterable(parsed)
                .filter(ParsedLine::isValid)
                .flatMap(this::resolveLine)
                .collectList()
                .flatMap(results -> {
                    List<ErrorUtil.ImportFailure> resolveFailures = results.stream()
                            .filter(r -> r.failure() != null)
                            .map(LineResult::failure)
                            .toList();

                    List<ErrorUtil.ImportFailure> allFailures = new ArrayList<>(parseFailures);
                    allFailures.addAll(resolveFailures);

                    if (!allFailures.isEmpty()) {
                        return Mono.error(importFailed(allFailures));
                    }

                    List<DeckCardEntity> commander = results.stream()
                            .filter(r -> r.line().section() == ParsedLine.Section.COMMANDER)
                            .map(LineResult::card)
                            .toList();
                    List<DeckCardEntity> cards = results.stream()
                            .filter(r -> r.line().section() != ParsedLine.Section.COMMANDER)
                            .map(LineResult::card)
                            .toList();

                    return Mono.just(new ResolvedImport(new ArrayList<>(commander), new ArrayList<>(cards)));
                });
    }

    private record LineResult(ParsedLine line, DeckCardEntity card, ErrorUtil.ImportFailure failure) {}

    private Mono<LineResult> resolveLine(ParsedLine line) {
        return libraryFeignClient.fetchCardByName(line.cardName())
                .map(card -> new LineResult(line, snapshot(card, line.count(), line.section().toCategory()), null))
                .onErrorResume(err -> {
                    if (err instanceof ResponseException re && re.getHttpStatus() == HttpStatus.NOT_FOUND) {
                        return Mono.just(new LineResult(line, null,
                                new ErrorUtil.ImportFailure(line.lineNumber(), line.originalLine(),
                                        "Card '" + line.cardName() + "' not found in library")));
                    }
                    return Mono.error(err);
                });
    }

    private DeckEntity assembleImportedDeck(Principal principal, ImportDeckRequest request, ResolvedImport resolved) {
        Instant now = Instant.now();
        var builder = DeckEntity.builder()
                .audit(AuditEntity.builder().createdAt(now).lastModified(now).build())
                .name(request.name())
                .description(request.description())
                .format(request.format())
                .status(request.deckStatus() != null ? request.deckStatus() : DeckStatus.DRAFT)
                .commander(resolved.commander())
                .cards(resolved.cards())
                .visibility(request.visibility() != null ? request.visibility() : Visibility.PRIVATE);

        return switch (principal.type()) {
            case USER -> builder.userId(principal.id()).build();
            case ANON -> builder.anonId(principal.id()).visibility(Visibility.PRIVATE).build();
        };
    }
}
