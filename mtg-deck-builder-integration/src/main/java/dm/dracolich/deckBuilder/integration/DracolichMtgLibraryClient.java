package dm.dracolich.deckBuilder.integration;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.response.DmdResponse;
import dm.dracolich.mtgLibrary.dto.CardDto;
import dm.dracolich.mtgLibrary.dto.GameFormatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static dm.dracolich.deckBuilder.integration.helpers.ErrorUtil.*;

@Component
@RequiredArgsConstructor
public class DracolichMtgLibraryClient {
    private final WebClient mtgLibraryWebClient;

    private static final String PATH_CARDS = "/cards";
    private static final String PATH_FORMATS = "/formats";
    /**
     * Retrieves card by given id number.
     *
     * @param id the card id
     * @return all the information for given card id
     */
    public Mono<CardDto> fetchCardById(String id) {
        return mtgLibraryWebClient.get()
                .uri(PATH_CARDS + "/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleLibraryClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleLibraryServerError)
                .bodyToMono(new ParameterizedTypeReference<DmdResponse<CardDto>>() { })
                .map(DmdResponse::getPayload);
    }

    /**
     * Retrieves a card by its name (case-insensitive).
     *
     * @param name the card name
     * @return card data, or empty if not found (404 from upstream is mapped to libraryNotFound)
     */
    public Mono<CardDto> fetchCardByName(String name) {
        return mtgLibraryWebClient.get()
                .uri(uriBuilder -> uriBuilder.path(PATH_CARDS).queryParam("name", name).build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleLibraryClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleLibraryServerError)
                .bodyToMono(new ParameterizedTypeReference<DmdResponse<CardDto>>() { })
                .map(DmdResponse::getPayload);
    }

    /**
     * Retrieves game format by given code.
     *
     * @param code the format name code, e.g. COMMANDER, STANDARD_BRAWL
     * @return all the information for requested game format
     */
    public Mono<GameFormatDto> fetchFormatByCode(String code) {
        return mtgLibraryWebClient.get()
                .uri(PATH_FORMATS + "/{code}", code)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleLibraryClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleLibraryServerError)
                .bodyToMono(new ParameterizedTypeReference<DmdResponse<GameFormatDto>>() { })
                .map(DmdResponse::getPayload);
    }

    private Mono<? extends Throwable> handleLibraryClientError(ClientResponse response) {
        return response.bodyToMono(new ParameterizedTypeReference<DmdResponse<Object>>() {})
                .defaultIfEmpty(new DmdResponse<>())
                .map(dmd -> {
                    String upstreamMessage = dmd.getMessage() != null
                            ? dmd.getMessage()
                            : "Library call failed";
                    List<ApiError> upstreamErrors = dmd.getErrors() != null
                            ? dmd.getErrors()
                            : List.of();

                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return libraryNotFound(upstreamMessage, upstreamErrors);
                    }
                    return libraryClientError(upstreamMessage, upstreamErrors);
                });
    }

    private Mono<? extends Throwable> handleLibraryServerError(ClientResponse response) {
        return response.releaseBody()
                .then(Mono.just(libraryUnavailable()));
    }
}
