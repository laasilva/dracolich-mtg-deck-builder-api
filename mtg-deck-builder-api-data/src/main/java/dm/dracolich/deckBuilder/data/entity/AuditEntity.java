package dm.dracolich.deckBuilder.data.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditEntity {
    private String version;
    @Field("created_at")
    private Instant createdAt;
    @Field("last_modified")
    private Instant lastModified;
    @Field("deleted_at")
    private Instant deletedAt;
}
