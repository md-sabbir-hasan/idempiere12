package org.mycompany.o2c.util;

import java.math.BigDecimal;

/** Small reusable checks. They throw {@link ValidationException} with a user friendly message. */
public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static boolean isId(int id) {
        return id > 0;
    }

    public static void requireId(int id, String message) {
        if (id <= 0)
            throw new ValidationException(message);
    }

    public static void requireNotNull(Object o, String message) {
        if (o == null)
            throw new ValidationException(message);
    }

    public static void requirePositive(BigDecimal v, String message) {
        if (v == null || v.signum() <= 0)
            throw new ValidationException(message);
    }

    /** Parses an id coming from a UI component; 0 when blank or not a number. */
    public static int parseId(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ex) {
            return 0;
        }
    }
}
