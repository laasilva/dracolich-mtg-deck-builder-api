package dm.dracolich.deckBuilder.web.controller;

import dm.dracolich.deckBuilder.core.service.FavoriteService;
import dm.dracolich.deckBuilder.dto.DeckDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("favorites")
@Tag(name = "Favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService service;

    @Operation(summary = "Favorite deck", description = "Add deck to user's favorites list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Deck added successfully to favorites list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Card not found"),
            @ApiResponse(responseCode = "503", description = "Internal server error")
    })
    @PostMapping(path = {""}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<?> favoriteDeck(@RequestParam(name = "deck_id") String deckId) {
        return service.favoriteDeck(deckId);
    }

    @Operation(summary = "Favorite deck", description = "Add deck to user's favorites list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deck added successfully to favorites list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Card not found"),
            @ApiResponse(responseCode = "503", description = "Internal server error")
    })
    @DeleteMapping(path = {""}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<?> unfavoriteDeck(@RequestParam(name = "deck_id") String deckId) {
        return service.unfavoriteDeck(deckId);
    }
}
