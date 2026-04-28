package dm.dracolich.deckBuilder.integration.helpers;

import dm.dracolich.deckBuilder.dto.error.ErrorCodes;
import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.exception.ResponseException;
import org.springframework.http.HttpStatus;

import java.util.List;

public final class ErrorUtil {

    private ErrorUtil() {
    }

    public static ResponseException libraryNotFound(String message, List<ApiError> upstreamErrors) {
        var error = new ApiError(ErrorCodes.DMD030);
        return new ResponseException("MTG Library lookup failed: " + message,
                List.of(error), HttpStatus.NOT_FOUND);
    }

    public static ResponseException libraryClientError(String message, List<ApiError> upstreamErrors) {
        var error = new ApiError(ErrorCodes.DMD031);
        return new ResponseException("MTG Library request rejected: " + message,
                List.of(error), HttpStatus.BAD_REQUEST);
    }

    public static ResponseException libraryUnavailable() {
        var error = new ApiError(ErrorCodes.DMD032);
        return new ResponseException("MTG Library API unavailable",
                List.of(error), HttpStatus.SERVICE_UNAVAILABLE);
    }
}
