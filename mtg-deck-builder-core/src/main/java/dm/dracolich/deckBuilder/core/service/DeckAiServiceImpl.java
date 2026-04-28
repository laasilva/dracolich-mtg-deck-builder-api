package dm.dracolich.deckBuilder.core.service;

import dm.dracolich.ai.dto.CardSuggestionDto;
import dm.dracolich.ai.dto.IssueDto;
import dm.dracolich.ai.dto.enums.SessionType;
import dm.dracolich.ai.dto.records.ChatRequest;
import dm.dracolich.ai.dto.records.DeckCardRequest;
import dm.dracolich.ai.dto.records.SessionCreateRequest;
import dm.dracolich.deckBuilder.core.stats.DeckStatsCalculator;
import dm.dracolich.deckBuilder.data.entity.AiCacheEntity;
import dm.dracolich.deckBuilder.data.entity.DeckCardEntity;
import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.data.repository.DeckRepository;
import dm.dracolich.deckBuilder.dto.DeckAiResultDto;
import dm.dracolich.deckBuilder.dto.DeckStatsDto;
import dm.dracolich.deckBuilder.integration.DracolichAiClient;
import dm.dracolich.forge.security.Principal;
import dm.dracolich.forge.security.ReactiveSecurityContextUtil;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static dm.dracolich.deckBuilder.core.helpers.ErrorUtil.notFound;
import static dm.dracolich.deckBuilder.core.helpers.ErrorUtil.unauthorized;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckAiServiceImpl implements DeckAiService {

    private static final String SUGGEST_PROMPT =
            "Based on my current deck, suggest specific cards I could add to improve it. " +
                    "Use the suggestCards tool to formally recommend each card with a category and reason. " +
                    "Then write a brief 2-3 sentence summary of your reasoning.";

    private static final String ANALYZE_PROMPT =
            "Analyze this deck thoroughly. Comment on the mana curve, color balance, " +
                    "synergies, weaknesses, and overall game plan. Use the suggestCards tool to recommend " +
                    "cards that address any weaknesses you identify. Be concise but specific.";

    private final DeckRepository repo;
    private final DracolichAiClient aiClient;

    @Override
    public Mono<DeckAiResultDto> suggestForDeck(String deckId) {
        return runCachedDeckOperation(deckId, SessionType.BUILD, SUGGEST_PROMPT,
                AiCacheEntity::getSuggestResult,
                AiCacheEntity::getSuggestAt,
                (cache, result) -> {
                    cache.setSuggestResult(result);
                    cache.setSuggestAt(Instant.now());
                });
    }

    @Override
    public Mono<DeckAiResultDto> analyzeDeck(String deckId) {
        return runCachedDeckOperation(deckId, SessionType.ANALYSIS, ANALYZE_PROMPT,
                AiCacheEntity::getAnalyzeResult,
                AiCacheEntity::getAnalyzeAt,
                (cache, result) -> {
                    cache.setAnalyzeResult(result);
                    cache.setAnalyzeAt(Instant.now());
                });
    }

    private Mono<DeckAiResultDto> runCachedDeckOperation(
            String deckId,
            SessionType type,
            String prompt,
            Function<AiCacheEntity, DeckAiResultDto> getCachedResult,
            Function<AiCacheEntity, Instant> getCachedAt,
            BiConsumer<AiCacheEntity, DeckAiResultDto> writeCache) {

        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .flatMap(principal -> loadOwnedDeck(deckId, principal))
                .flatMap(deck -> {
                    DeckAiResultDto cached = isCacheValid(deck, getCachedAt) && deck.getAiCache() != null
                            ? getCachedResult.apply(deck.getAiCache())
                            : null;
                    if (cached != null) {
                        log.info("AI cache HIT for deck {} ({})", deck.getId(), type);
                        return Mono.just(cached);
                    }
                    log.info("AI cache MISS for deck {} ({}), calling ai-api", deck.getId(), type);
                    return streamAndAssemble(deck, type, prompt)
                            .doOnNext(result -> persistCache(deck, result, writeCache));
                });
    }

    private boolean isCacheValid(DeckEntity deck, Function<AiCacheEntity, Instant> getCachedAt) {
        if (deck.getAiCache() == null) return false;
        Instant cachedAt = getCachedAt.apply(deck.getAiCache());
        if (cachedAt == null) return false;
        Instant lastModified = deck.getAudit() != null ? deck.getAudit().getLastModified() : null;
        if (lastModified == null) return true;
        return !lastModified.isAfter(cachedAt);
    }

    /**
     * Calls ai-api: stream chat, buffer the text into a summary, then fetch the session
     * to pull out structured cardSuggestions and analysisIssues populated by the AI's tools.
     *
     * <p>Stats are computed locally and prepended to the prompt — the AI never needs to call
     * a stats tool, which saves ~70 mtg-library-api round trips and ~2-5 seconds per call.</p>
     */
    private Mono<DeckAiResultDto> streamAndAssemble(DeckEntity deck, SessionType type, String prompt) {
        String fullPrompt = statsContext(deck) + "\n\n" + prompt;
        return getOrCreateSession(deck, type)
                .flatMap(sessionId -> aiClient.chatStream(new ChatRequest(sessionId, fullPrompt))
                        .reduce(new StringBuilder(), StringBuilder::append)
                        .map(StringBuilder::toString)
                        .timeout(Duration.ofSeconds(60))
                        .flatMap(summary -> aiClient.getSession(sessionId)
                                .timeout(Duration.ofSeconds(60))
                                .map(session -> DeckAiResultDto.builder()
                                        .summary(summary)
                                        .issues(session.getAnalysisIssues() != null
                                                ? session.getAnalysisIssues()
                                                : List.<IssueDto>of())
                                        .suggestions(session.getCardSuggestions() != null
                                                ? session.getCardSuggestions()
                                                : List.<CardSuggestionDto>of())
                                        .build())));
    }

    /**
     * Builds a deterministic stats summary block that's prepended to AI prompts.
     * Replaces the AI's old DeckAnalysisTool call by giving it the same data
     * up-front — exact, instant, free of HTTP overhead.
     */
    private static String statsContext(DeckEntity deck) {
        DeckStatsDto stats = DeckStatsCalculator.compute(deck);
        StringBuilder sb = new StringBuilder("Pre-computed deck stats (use these instead of estimating):\n");
        sb.append("- Total cards: ").append(stats.cardCount()).append("\n");
        sb.append("- Land count: ").append(stats.landCount()).append("\n");
        if (stats.averageCmc() != null) {
            sb.append("- Average CMC: ").append(stats.averageCmc()).append("\n");
        }
        if (stats.manaCurve() != null && !stats.manaCurve().isEmpty()) {
            sb.append("- Mana curve (CMC: count): ").append(stats.manaCurve()).append("\n");
        }
        if (stats.colorPie() != null && !stats.colorPie().isEmpty()) {
            sb.append("- Color distribution: ").append(stats.colorPie()).append("\n");
        }
        if (stats.typeBreakdown() != null && !stats.typeBreakdown().isEmpty()) {
            sb.append("- Type breakdown: ").append(stats.typeBreakdown()).append("\n");
        }
        if (stats.warnings() != null && !stats.warnings().isEmpty()) {
            sb.append("- Pre-computed warnings: ").append(String.join("; ", stats.warnings())).append("\n");
        }
        return sb.toString();
    }

    private void persistCache(DeckEntity deck, DeckAiResultDto result, BiConsumer<AiCacheEntity, DeckAiResultDto> writeCache) {
        if (result == null || result.getSummary() == null || result.getSummary().isEmpty()) return;
        if (deck.getAiCache() == null) {
            deck.setAiCache(AiCacheEntity.builder().build());
        }
        writeCache.accept(deck.getAiCache(), result);
        repo.save(deck)
                .doOnError(e -> log.warn("Failed to cache AI response for deck {}: {}", deck.getId(), e.getMessage()))
                .subscribe();
    }

    @Override
    public Flux<String> suggestForCard(String cardId, Format format) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .flatMap(principal -> {
                    if (!principal.isUser()) return Mono.error(unauthorized());
                    SessionCreateRequest req = new SessionCreateRequest(
                            principal.id(),
                            SessionType.ANALYSIS,
                            format.name(),
                            null,
                            List.of(new DeckCardRequest(cardId, "MAINBOARD", 1, false)));
                    return aiClient.createSession(req);
                })
                .flatMapMany(session -> aiClient.chatStream(new ChatRequest(
                        session.getId(),
                        "What cards synergize well with this card in " + format.name() +
                                "? Suggest 3-5 specific cards using the suggestCards tool, with a reason for each.")));
    }

    private Mono<DeckEntity> loadOwnedDeck(String deckId, Principal principal) {
        if (!principal.isUser()) {
            return Mono.error(unauthorized());
        }
        return repo.findByIdAndUserId(deckId, principal.id())
                .switchIfEmpty(Mono.error(notFound("ai_deck", "deck", deckId)));
    }

    private Mono<String> getOrCreateSession(DeckEntity deck, SessionType type) {
        if (deck.getAiSessionId() != null) {
            return Mono.just(deck.getAiSessionId());
        }
        SessionCreateRequest req = new SessionCreateRequest(
                deck.getUserId(),
                type,
                deck.getFormat() != null ? deck.getFormat().name() : "COMMANDER",
                commanderName(deck),
                deckList(deck));
        return aiClient.createSession(req)
                .flatMap(session -> {
                    deck.setAiSessionId(session.getId());
                    return repo.save(deck).thenReturn(session.getId());
                });
    }

    private static String commanderName(DeckEntity deck) {
        if (deck.getCommander() == null || deck.getCommander().isEmpty()) return null;
        DeckCardEntity first = deck.getCommander().getFirst();
        return first != null ? first.getName() : null;
    }

    private static List<DeckCardRequest> deckList(DeckEntity deck) {
        if (deck.getCards() == null) return List.of();
        return deck.getCards().stream()
                .map(c -> new DeckCardRequest(
                        c.getName(),
                        c.getCardCategory() != null ? c.getCardCategory().name() : "MAINBOARD",
                        c.getCount() != null ? c.getCount() : 1,
                        false))
                .toList();
    }
}
