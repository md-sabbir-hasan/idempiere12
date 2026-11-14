package org.mycompany.o2c.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.mycompany.o2c.dao.TaxDAO;
import org.mycompany.o2c.util.ValidationUtils;

/**
 * Tax / VAT. The final VAT determination policy is not approved yet, so this service keeps
 * the two decisions in one place:
 * <ul>
 * <li>which rate applies: product tax category -> C_Tax (customer VAT rate is display only);</li>
 * <li>what VAT is charged on: the gross amount (paid qty x price, before discount, free qty not taxed).</li>
 * </ul>
 */
public class TaxService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private final TaxDAO dao = new TaxDAO();

    /** Rate (percent) for the tax category; 0 when no category or no valid tax is configured. */
    public BigDecimal getRate(int taxCategoryId) {
        if (!ValidationUtils.isId(taxCategoryId))
            return BigDecimal.ZERO;
        BigDecimal r = dao.findRate(taxCategoryId);
        return r == null ? BigDecimal.ZERO : r;
    }

    /** VAT on the gross amount, rounded to 2 decimals. */
    public BigDecimal vatOnGross(BigDecimal gross, BigDecimal ratePercent) {
        return gross.multiply(ratePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
