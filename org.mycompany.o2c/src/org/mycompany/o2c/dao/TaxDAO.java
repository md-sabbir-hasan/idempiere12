package org.mycompany.o2c.dao;

import java.math.BigDecimal;

import org.mycompany.o2c.util.DBUtils;

/** Database access for C_Tax. */
public class TaxDAO {

    /**
     * Rate of the default valid tax of the tax category (sales or both).
     * Simple lookup only: country / exemption based tax determination is not done here yet.
     * Null when no tax is configured.
     */
    public BigDecimal findRate(int taxCategoryId) {
        return DBUtils.queryBigDecimal(
            "SELECT COALESCE(Rate,0) FROM C_Tax WHERE C_TaxCategory_ID=? AND IsActive='Y' "
          + "AND ValidFrom<=now() AND SOPOType IN ('B','S') "
          + "ORDER BY IsDefault DESC, ValidFrom DESC LIMIT 1",
            taxCategoryId);
    }
}
