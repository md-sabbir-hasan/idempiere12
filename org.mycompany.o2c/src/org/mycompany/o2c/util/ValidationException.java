package org.mycompany.o2c.util;

/**
 * A business / input validation failure. The message is meant to be shown to the user as is.
 */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
