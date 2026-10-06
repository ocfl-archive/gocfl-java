package org.ocflarchive.gocfl.exception;

/**
 * Exception thrown when a gocfl operation (init, add, update, create) returns failure.
 */
public class GocflOperationException extends GocflException {

    public GocflOperationException(String message) {
        super(message);
    }

    public GocflOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
