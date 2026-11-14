package org.mycompany.o2c.model;

import java.math.BigDecimal;

/** Totals shown under the order lines. */
public class OrderTotals {
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal vat;
    private final BigDecimal grandTotal;

    public OrderTotals(BigDecimal subtotal, BigDecimal discount, BigDecimal vat, BigDecimal grandTotal) {
        this.subtotal = subtotal;
        this.discount = discount;
        this.vat = vat;
        this.grandTotal = grandTotal;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getVat() {
        return vat;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }
}
