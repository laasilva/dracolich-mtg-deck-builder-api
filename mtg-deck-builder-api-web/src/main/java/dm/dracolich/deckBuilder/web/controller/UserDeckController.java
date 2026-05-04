package dm.dracolich.deckBuilder.web.controller;

import dm.dracolich.deckBuilder.core.service.DeckService;
import dm.dracolich.deckBuilder.dto.DeckDto;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("users")
@Tag(name = "User profile")
@RequiredArgsConstructor
public class UserDeckController {
    private final DeckService service;

    @Operation(summary = "List a user's public decks", description = "Public profile listing of a user's PUBLIC decks. Optional format filter, paginated, sorted by last_modified DESC.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decks retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @GetMapping(path = {"/{userId}/decks"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Page<DeckDto>> listPublicUserDecks(
            @PathVariable String userId,
            @RequestParam(required = false) Format format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listPublicUserDecks(userId, format, page, size);
    }

    @Operation(summary = "List a user's favorited public decks", description = "Public profile listing of decks the user has favorited, filtered to PUBLIC only. Paginated, sorted by last_modified DESC.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Favorites retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DeckDto.class))),
            @ApiResponse(responseCode = "503", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = DeckDto.class)))
    })
    @GetMapping(path = {"/{userId}/favorites"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Page<DeckDto>> listPublicUserFavorites(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listPublicUserFavorites(userId, page, size);
    }
}
