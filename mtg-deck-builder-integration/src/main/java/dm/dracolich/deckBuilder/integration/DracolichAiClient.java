package dm.dracolich.deckBuilder.integration;

import dm.dracolich.ai.dto.SessionDto;
import dm.dracolich.ai.dto.records.ChatRequest;
import dm.dracolich.ai.dto.records.SessionCreateRequest;
import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.response.DmdResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static dm.dracolich.deckBuilder.integration.helpers.ErrorUtil.libraryClientError;
import static dm.dracolich.deckBuilder.integration.helpers.ErrorUtil.libraryNotFound;
import static dm.dracolich.deckBuilder.integration.helpers.ErrorUtil.libraryUnavailable;

@Component
@RequiredArgsConstructor
public class DracolichAiClient {
    private final WebClient aiApiWebClient;

    private static final String PATH_SESSION = "/agent/session";
    private static final String PATH_CHAT = "/agent/chat";

    /**
     * Creates a new ai-api session for the given context (BUILD or ANALYSIS).
     */
    public Mono<SessionDto> createSession(SessionCreateRequest request) {
        return aiApiWebClient.post()
                .uri(PATH_SESSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleAiClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleAiServerError)
                .bodyToMono(new ParameterizedTypeReference<DmdResponse<SessionDto>>() { })
                .map(DmdResponse::getPayload);
    }

    /**
     * Fetches a session by id from ai-api. Used after streaming completes to retrieve
     * structured fields (cardSuggestions, future analysisIssues) the AI populated via tool calls.
     */
    public Mono<SessionDto> getSession(String sessionId) {
        return aiApiWebClient.get()
                .uri(PATH_SESSION + "/{id}", sessionId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleAiClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleAiServerError)
                .bodyToMono(new ParameterizedTypeReference<DmdResponse<SessionDto>>() { })
                .map(DmdResponse::getPayload);
    }

    /**
     * Streams the AI's response to a chat message via SSE pass-through.
     * Each emission is one chunk of the AI's response text.
     */
    public Flux<String> chatStream(ChatRequest request) {
        return aiApiWebClient.post()
                .uri(PATH_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleAiClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleAiServerError)
                .bodyToFlux(String.class);
    }

    private Mono<? extends Throwable> handleAiClientError(ClientResponse response) {
        return response.bodyToMono(new ParameterizedTypeReference<DmdResponse<Object>>() {})
                .defaultIfEmpty(new DmdResponse<>())
                .map(dmd -> {
                    String upstreamMessage = dmd.getMessage() != null
                            ? dmd.getMessage()
                            : "AI call failed";
                    List<ApiError> upstreamErrors = dmd.getErrors() != null
                            ? dmd.getErrors()
                            : List.of();

                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return libraryNotFound(upstreamMessage, upstreamErrors);
                    }
                    return libraryClientError(upstreamMessage, upstreamErrors);
                });
    }

    private Mono<? extends Throwable> handleAiServerError(ClientResponse response) {
        return response.releaseBody()
                .then(Mono.just(libraryUnavailable()));
    }
}
