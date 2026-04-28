package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditDto {
    private String version;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("last_modified")
    private Instant lastModified;
    @JsonProperty("deleted_at")
    private Instant deletedAt;
}
