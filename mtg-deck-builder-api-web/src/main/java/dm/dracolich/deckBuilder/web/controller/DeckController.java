package dm.dracolich.deckBuilder.web.controller;

import dm.dracolich.deckBuilder.core.service.DeckService;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("decks")
@Tag(name = "Decks")
@RequiredArgsConstructor
public class DeckController {
    private final DeckService service;

    @Operation(summary = "Create a new deck", description = "Creates a new deck")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Deck created successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @PostMapping(path = {"/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckDto> createDeck(@Valid @RequestBody CreateDeckRequest request) {
        return service.createDeck(request);
    }

    @Operation(summary = "Add card to deck", description = "Add a new card to a existing deck")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card added successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @PostMapping(path = {"/{id}/cards"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckDto> addCardToDeck(@PathVariable String id,
                                 @Valid @RequestBody CardRequest request) {
        return service.addCardToDeck(id, request);
    }

    @Operation(summary = "Copy deck", description = "Copy existing deck to user's account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Deck copied successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @PostMapping(path = {"/{id}/copy"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckDto> copyDeck(@PathVariable String id) {
        return service.copyDeck(id);
    }

    @Operation(summary = "List user's decks", description = "Paginated list of decks owned by the authenticated user, with optional filters by format, status, and visibility. Sorted by last modified DESC.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decks retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @GetMapping(path = {"/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Page<DeckDto>> listUserDecks(
            @RequestParam(required = false) Format format,
            @RequestParam(required = false) DeckStatus status,
            @RequestParam(required = false) Visibility visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listUserDecks(format, status, visibility, page, size);
    }

    @Operation(summary = "List popular public decks", description = "Public decks ranked by favorites count (DESC), tie-broken by created_at DESC. Optional format filter.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decks retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @GetMapping(path = {"/popular"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Page<DeckDto>> listPopularDecks(
            @RequestParam(required = false) Format format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listPopularDecks(format, page, size);
    }

    @Operation(summary = "List latest public decks", description = "Public decks sorted by created_at DESC. Optional format filter.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decks retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @GetMapping(path = {"/latest"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Page<DeckDto>> listLatestDecks(
            @RequestParam(required = false) Format format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listLatestDecks(format, page, size);
    }

    @Operation(summary = "Fetch deck by Id", description = "Fetch an existing deck by Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deck retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "404", description = "Deck not found",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @GetMapping(path = {"/{id}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckDto> fetchDeckById(@PathVariable String id) {
        return service.fetchDeckById(id);
    }

    @Operation(summary = "Delete deck", description = "Delete deck from user's library")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deck deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Deck not found"),
            @ApiResponse(responseCode = "503", description = "Internal server error")
    })
    @DeleteMapping(path = {"/{id}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<?> deleteDeck(@PathVariable String id) {
        return service.deleteDeckById(id);
    }

    @Operation(summary = "Remove card from deck", description = "Remove a card from a existing deck")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card removed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Card not found"),
            @ApiResponse(responseCode = "503", description = "Internal server error")
    })
    @DeleteMapping(path = {"/{id}/cards/{cardId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<?> deleteCardFromDeck(@PathVariable String id,
                                 @PathVariable String cardId) {
        return service.removeCardFromDeck(id, cardId);
    }

    @Operation(summary = "Validate an unsaved deck", description = "Returns full validation + stats for a deck submitted in the request body, without persisting. Useful for anon users wanting a quick analysis.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analysis computed successfully",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class))),
            @ApiResponse(responseCode = "503", description = "MTG Library unavailable",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class)))
    })
    @PostMapping(path = {"/validate"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckAnalysisDto> validateUnsavedDeck(@Valid @RequestBody ValidateDeckRequest request) {
        return service.validateUnsavedDeck(request);
    }

    @Operation(summary = "Get deck stats", description = "Deterministic stats (mana curve, color pie, type breakdown, average CMC, deterministic warnings) for a saved deck. No AI.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stats retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DeckStatsDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = DeckStatsDto.class))),
            @ApiResponse(responseCode = "404", description = "Deck not found",
                    content = @Content(schema = @Schema(implementation = DeckStatsDto.class)))
    })
    @GetMapping(path = {"/{id}/stats"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckStatsDto> fetchDeckStats(@PathVariable String id) {
        return service.fetchDeckStats(id);
    }

    @Operation(summary = "Validate a saved deck", description = "Runs format rules validation against a saved deck. No AI.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation computed successfully",
                    content = @Content(schema = @Schema(implementation = DeckValidationDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = DeckValidationDto.class))),
            @ApiResponse(responseCode = "404", description = "Deck not found",
                    content = @Content(schema = @Schema(implementation = DeckValidationDto.class)))
    })
    @GetMapping(path = {"/{id}/validate"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckValidationDto> fetchDeckValidation(@PathVariable String id) {
        return service.fetchDeckValidation(id);
    }

    @Operation(summary = "Import a deck from text", description = "Parses a Scryfall/Moxfield-style decklist (count + name per line, // section markers) and persists as a new deck. Strict mode: any unresolved card fails the entire import with per-line error messages.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Deck imported successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "422", description = "Import failed (per-line errors in response)",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "MTG Library unavailable",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @PostMapping(path = {"/import"},
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckDto> importDeck(@Valid @RequestBody ImportDeckRequest request) {
        return service.importDeck(request);
    }

    @Operation(summary = "Import a deck from text (plain text body)", description = "Same as POST /decks/import but accepts a raw text/plain body — useful for pasting decklists without JSON escaping. Metadata travels as query params.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Deck imported successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "422", description = "Import failed (per-line errors in response)",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "MTG Library unavailable",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @PostMapping(path = {"/import"},
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckDto> importDeckFromText(
            @RequestParam Format format,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, name = "deck_status") DeckStatus deckStatus,
            @RequestParam(required = false) Visibility visibility,
            @RequestBody String deckText) {
        return service.importDeck(new ImportDeckRequest(format, name, description, deckStatus, visibility, deckText));
    }

    @Operation(summary = "Export a deck to text", description = "Returns the deck in Scryfall-compatible text format (cards before any marker = mainboard, // Commander / // Sideboard / // Maybeboard sections). Visibility-respecting: private decks require ownership.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deck exported successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Deck not found")
    })
    @GetMapping(path = {"/{id}/export"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> exportDeck(@PathVariable String id) {
        return service.exportDeck(id);
    }

    @Operation(summary = "Validate an imported deck text", description = "Parses a decklist and returns full validation + stats without persisting. Strict mode: any unresolved card fails with per-line error messages.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analysis computed successfully",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class))),
            @ApiResponse(responseCode = "422", description = "Import failed (per-line errors in response)",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class))),
            @ApiResponse(responseCode = "503", description = "MTG Library unavailable",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class)))
    })
    @PostMapping(path = {"/import/validate"},
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckAnalysisDto> validateImportedDeck(@Valid @RequestBody ImportValidateRequest request) {
        return service.validateImportedDeck(request);
    }

    @Operation(summary = "Validate an imported deck text (plain text body)", description = "Same as POST /decks/import/validate but accepts a raw text/plain body — useful for pasting decklists without JSON escaping. Format travels as a query param.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analysis computed successfully",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class))),
            @ApiResponse(responseCode = "422", description = "Import failed (per-line errors in response)",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class))),
            @ApiResponse(responseCode = "503", description = "MTG Library unavailable",
                    content = @Content(schema = @Schema(implementation = DeckAnalysisDto.class)))
    })
    @PostMapping(path = {"/import/validate"},
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DeckAnalysisDto> validateImportedDeckFromText(
            @RequestParam Format format,
            @RequestBody String deckText) {
        return service.validateImportedDeck(new ImportValidateRequest(format, deckText));
    }
}
