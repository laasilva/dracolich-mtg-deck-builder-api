package dm.dracolich.deckBuilder.web.controller;

import dm.dracolich.deckBuilder.core.service.DeckAiService;
import dm.dracolich.deckBuilder.dto.DeckAiResultDto;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("ai")
@Tag(name = "AI")
@RequiredArgsConstructor
public class AiController {

    private final DeckAiService service;

    @Operation(summary = "Request AI card suggestions for a deck",
            description = "Returns the AI's text summary plus structured card suggestions extracted via the suggestCards tool. " +
                    "Creates or reuses an ai-api session per deck. Cached: subsequent calls without deck modifications return the previous response.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI result returned successfully",
                    content = @Content(schema = @Schema(implementation = DeckAiResultDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Deck not found"),
            @ApiResponse(responseCode = "503", description = "AI API unavailable")
    })
    @PostMapping(path = "/decks/{id}/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckAiResultDto> suggestForDeck(@PathVariable String id) {
        return service.suggestForDeck(id);
    }

    @Operation(summary = "Request AI deck analysis",
            description = "Returns the AI's text summary plus structured card suggestions extracted via the suggestCards tool. " +
                    "Creates or reuses an ai-api session per deck. Cached: subsequent calls without deck modifications return the previous response.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI result returned successfully",
                    content = @Content(schema = @Schema(implementation = DeckAiResultDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Deck not found"),
            @ApiResponse(responseCode = "503", description = "AI API unavailable")
    })
    @PostMapping(path = "/decks/{id}/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckAiResultDto> analyzeDeck(@PathVariable String id) {
        return service.analyzeDeck(id);
    }

    @Operation(summary = "Request AI synergy suggestion for a single card",
            description = "Stateless one-shot: creates a transient ai-api session, asks for cards that synergize with the given card in the given format, streams the response. Session expires via ai-api TTL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI response streamed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "503", description = "AI API unavailable")
    })
    @PostMapping(path = "/cards/{cardId}/format/{format}/suggest", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> suggestForCard(@PathVariable String cardId, @PathVariable Format format) {
        return service.suggestForCard(cardId, format);
    }
}
