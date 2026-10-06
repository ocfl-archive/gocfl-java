package org.ocflarchive.gocfl.exception;

/**
 * Base exception for all gocfl-related errors.
 */
public class GocflException extends RuntimeException {

    public GocflException(String message) {
        super(message);
    }

    public GocflException(String message, Throwable cause) {
        super(message, cause);
    }
}
