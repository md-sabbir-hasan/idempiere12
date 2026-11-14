package org.mycompany.o2c.service;

import java.math.BigDecimal;

import org.mycompany.o2c.model.SalesOrderLine;

/**
 * Discount Management entry point (product / category, customer, special, quantity, Buy X Get Y).
 *
 * No discount is applied yet: the combination policy (additive or sequential, best offer wins)
 * and the special discount approval limit are not approved, so nothing is invented here.
 * All discount rules will be added in this class so that SalesOrderService and the UI do not change.
 */
public class DiscountService {

    /** Discount amount for the line (currency). Currently always 0. */
    public BigDecimal calculateDiscount(SalesOrderLine line) {
        return BigDecimal.ZERO;
    }
}
