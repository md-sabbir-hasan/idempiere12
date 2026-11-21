package org.mycompany.o2c.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** One row of the order list. */
public record OrderRow(int orderId, String documentNo, Timestamp dateOrdered, String customer,
        String territory, String salesType, String segment, BigDecimal grandTotal, String docStatus,
        String waitingFor) {
}
