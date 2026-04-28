package dm.dracolich.deckBuilder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dm.dracolich.ai.dto.CardSuggestionDto;
import dm.dracolich.ai.dto.IssueDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured response from AI deck operations (suggest / analyze).
 * <ul>
 *   <li>{@code summary} — friendly text from the AI describing its findings.</li>
 *   <li>{@code issues} — structured analysis findings (mana_curve, color_balance, etc.) populated
 *       via the reportIssues tool. Renderable as colored badges/cards by the frontend.</li>
 *   <li>{@code suggestions} — structured cards the AI recommended via the suggestCards tool.
 *       Frontend should render these as cards, not parse them out of the summary.</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeckAiResultDto {
    private String summary;
    private List<IssueDto> issues;
    private List<CardSuggestionDto> suggestions;
}
