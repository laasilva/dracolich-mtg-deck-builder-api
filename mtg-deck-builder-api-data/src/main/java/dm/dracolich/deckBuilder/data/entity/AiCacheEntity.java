package dm.dracolich.deckBuilder.data.entity;

import dm.dracolich.deckBuilder.dto.DeckAiResultDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Per-deck cache of AI responses. Populated by deck-builder-api after a successful
 * AI call; served back when the deck hasn't been modified since the cache was written.
 * Server-side only — not surfaced on DeckDto.
 *
 * Stores the full structured DeckAiResultDto (summary + suggestions) so cache hits
 * can return identical shape to live calls without an additional ai-api round trip.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiCacheEntity {
    @Field("suggest_result")
    private DeckAiResultDto suggestResult;
    @Field("suggest_at")
    private Instant suggestAt;
    @Field("analyze_result")
    private DeckAiResultDto analyzeResult;
    @Field("analyze_at")
    private Instant analyzeAt;
}
