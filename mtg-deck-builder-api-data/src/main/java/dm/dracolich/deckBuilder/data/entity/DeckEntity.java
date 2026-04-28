package dm.dracolich.deckBuilder.data.entity;

import dm.dracolich.deckBuilder.dto.enums.DeckStatus;
import dm.dracolich.deckBuilder.dto.enums.Visibility;
import dm.dracolich.mtgLibrary.dto.enums.Color;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Document(collection = "decks")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeckEntity {
    @Id
    private String id;
    @Field("copied_from_id")
    private String copiedFromId; // if deck was copied
    @Field("user_id")
    private String userId;
    @Field("anon_id")
    private String anonId;
    private AuditEntity audit;
    private String name;
    private String description;
    private Set<Color> colors; // color id for commander
    private Format format;
    private DeckStatus status;
    private List<DeckCardEntity> commander; // only if commander deck - some can have partners
    @Builder.Default
    private List<DeckCardEntity> cards = new ArrayList<>(); // limit size according to format
    @Builder.Default
    @Field("favorites_count")
    private Long favoritesCount = 0L;
    private Visibility visibility;
    @Field("ai_session_id")
    private String aiSessionId;
    @Field("ai_cache")
    private AiCacheEntity aiCache;
}
