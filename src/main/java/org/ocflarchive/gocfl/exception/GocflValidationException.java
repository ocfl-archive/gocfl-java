package org.ocflarchive.gocfl.exception;

import org.ocflarchive.gocfl.proto.ValidateResult;

/**
 * Exception thrown when validation fails on an OCFL storage root or object.
 */
public class GocflValidationException extends GocflException {

    private final ValidateResult validateResult;

    public GocflValidationException(String message, ValidateResult validateResult) {
        super(message);
        this.validateResult = validateResult;
    }

    public ValidateResult getValidateResult() {
        return validateResult;
    }
}
