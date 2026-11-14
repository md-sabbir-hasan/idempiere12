package org.mycompany.o2c.model;

import java.math.BigDecimal;

/** Stock of one product in one warehouse. Available = On Hand - Reserved. */
public class StockInfo {
    public static final StockInfo EMPTY = new StockInfo(BigDecimal.ZERO, BigDecimal.ZERO);

    private final BigDecimal onHand;
    private final BigDecimal reserved;

    public StockInfo(BigDecimal onHand, BigDecimal reserved) {
        this.onHand = onHand == null ? BigDecimal.ZERO : onHand;
        this.reserved = reserved == null ? BigDecimal.ZERO : reserved;
    }

    public BigDecimal getOnHand() {
        return onHand;
    }

    public BigDecimal getReserved() {
        return reserved;
    }

    public BigDecimal getAvailable() {
        return onHand.subtract(reserved);
    }

    /** Quantity that cannot be covered by available stock (0 when stock is enough). */
    public BigDecimal shortageFor(BigDecimal requested) {
        if (requested == null)
            return BigDecimal.ZERO;
        BigDecimal s = requested.subtract(getAvailable());
        return s.signum() > 0 ? s : BigDecimal.ZERO;
    }
}
