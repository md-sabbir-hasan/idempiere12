package org.mycompany.o2c.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.mycompany.o2c.model.SalesOrderLine;
import org.mycompany.o2c.util.ValidationException;

/**
 * Discount Management entry point (product / category, customer, special, quantity, Buy X Get Y).
 *
 * Implemented now: Special Discount (manual, percent of the line gross, entered on the form).
 * Not approved yet, so nothing is invented: the combination policy with the other discounts
 * (additive or sequential, best offer wins) and the special discount APPROVAL LIMIT.
 * Set MAX_SPECIAL_DISCOUNT_PCT when the approval limit is decided.
 * All discount rules are added in this class so that SalesOrderService and the UI do not change.
 */
public class DiscountService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** Highest special discount a user may enter without approval. 100 = no limit decided yet. */
    public static final BigDecimal MAX_SPECIAL_DISCOUNT_PCT = HUNDRED;

    /** Validates the special discount percent (blank is treated as 0). */
    public BigDecimal validateSpecialDiscountPct(BigDecimal pct) {
        if (pct == null)
            return BigDecimal.ZERO;
        if (pct.signum() < 0)
            throw new ValidationException("Special Discount cannot be negative.");
        if (pct.compareTo(MAX_SPECIAL_DISCOUNT_PCT) > 0)
            throw new ValidationException("Special Discount cannot be more than "
                    + MAX_SPECIAL_DISCOUNT_PCT.stripTrailingZeros().toPlainString() + "%.");
        return pct;
    }

    /** Discount amount for the line (currency): special discount % of the gross amount. */
    public BigDecimal calculateDiscount(SalesOrderLine line) {
        BigDecimal pct = line.getSpecialDiscountPct();
        if (pct == null || pct.signum() <= 0 || line.getGross() == null)
            return BigDecimal.ZERO;
        return line.getGross().multiply(pct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
