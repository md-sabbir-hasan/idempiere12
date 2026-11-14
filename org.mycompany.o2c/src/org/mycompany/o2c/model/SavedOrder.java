package org.mycompany.o2c.model;

import java.math.BigDecimal;

/** What iDempiere reports back after the Draft was stored (real values, read from MOrder). */
public class SavedOrder {
    private final int orderId;
    private final String documentNo;
    private final String docStatus;
    private final BigDecimal totalLines;
    private final BigDecimal grandTotal;

    public SavedOrder(int orderId, String documentNo, String docStatus, BigDecimal totalLines,
            BigDecimal grandTotal) {
        this.orderId = orderId;
        this.documentNo = documentNo;
        this.docStatus = docStatus;
        this.totalLines = totalLines;
        this.grandTotal = grandTotal;
    }

    public int getOrderId() { return orderId; }
    public String getDocumentNo() { return documentNo; }
    public String getDocStatus() { return docStatus; }
    public BigDecimal getTotalLines() { return totalLines; }
    public BigDecimal getGrandTotal() { return grandTotal; }
}
