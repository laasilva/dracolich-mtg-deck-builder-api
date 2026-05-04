package dm.dracolich.deckBuilder.core.helpers;

import dm.dracolich.deckBuilder.dto.error.ErrorCodes;
import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorSeverity;
import dm.dracolich.forge.exception.ResponseException;
import org.springframework.http.HttpStatus;

import java.util.List;

public final class ErrorUtil {

    private ErrorUtil() {
    }

    public static ResponseException internalServerError(String context, String entity, String identifier) {
        var error = new ApiError(ErrorCodes.DMD033);
        return new ResponseException(ErrorCodes.DMD033.format(context, entity, identifier),
                List.of(error), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static ResponseException notFound(String context, String entity, String identifier) {
        var error = new ApiError(ErrorCodes.DMD033);
        return new ResponseException(ErrorCodes.DMD033.format(context, entity, identifier),
                List.of(error), HttpStatus.NOT_FOUND);
    }

    public static ResponseException unauthorized() {
        var error = new ApiError(ErrorCodes.DMD034);
        return new ResponseException(ErrorCodes.DMD034.getMessage(),
                List.of(error), HttpStatus.UNAUTHORIZED);
    }

    public static ResponseException deckAlreadyClaimed(String deckId) {
        var error = new ApiError(ErrorCodes.DMD036);
        return new ResponseException(ErrorCodes.DMD036.format(deckId),
                List.of(error), HttpStatus.CONFLICT);
    }

    public static ResponseException importFailed(List<ImportFailure> failures) {
        List<ApiError> errors = failures.stream()
                .map(f -> new ApiError(
                        ErrorCodes.DMD035,
                        ErrorSeverity.ERROR,
                        "line " + f.lineNumber() + ": '" + f.originalLine() + "' — " + f.reason()))
                .toList();
        return new ResponseException(
                "Deck import failed: " + failures.size() + " line(s) could not be processed",
                errors, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    public record ImportFailure(int lineNumber, String originalLine, String reason) { }
}
