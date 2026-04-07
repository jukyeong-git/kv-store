package io.github.jukyeong.kvstore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception to indicate that the service is temporarily unavailable
 * (e.g., due to infrastructure failures like Redis timeout).
 * Automatically translates to an HTTP 503 Service Unavailable response.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends RuntimeException {

    // Constructor to pass the custom error message to the parent RuntimeException
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
