package org.ocflarchive.gocfl.exception;

/**
 * Exception thrown when the gocfl-grpc server process fails to start, crashes, or encounters errors.
 */
public class GocflServerException extends GocflException {

    public GocflServerException(String message) {
        super(message);
    }

    public GocflServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
