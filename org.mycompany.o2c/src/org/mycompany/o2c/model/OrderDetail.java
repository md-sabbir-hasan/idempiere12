package org.mycompany.o2c.model;

import java.util.ArrayList;
import java.util.List;

/** A stored order read back for the form: header, lines and where it stands. */
public class OrderDetail {
    private int orderId;
    private String documentNo = "";
    private String docStatus = "";
    private String waitingFor = "";
    private String returnedInfo = "";
    private OrderHeader header = new OrderHeader();
    private List<SalesOrderLine> lines = new ArrayList<SalesOrderLine>();

    public int orderId() { return orderId; }
    public void setOrderId(int v) { orderId = v; }
    public String documentNo() { return documentNo; }
    public void setDocumentNo(String v) { documentNo = v == null ? "" : v; }
    public String docStatus() { return docStatus; }
    public void setDocStatus(String v) { docStatus = v == null ? "" : v; }
    public String waitingFor() { return waitingFor; }
    public void setWaitingFor(String v) { waitingFor = v == null ? "" : v; }
    /** Draft again after a rejection: who returned it and why; empty otherwise. */
    public String returnedInfo() { return returnedInfo; }
    public void setReturnedInfo(String v) { returnedInfo = v == null ? "" : v; }
    public OrderHeader header() { return header; }
    public void setHeader(OrderHeader v) { header = v; }
    public List<SalesOrderLine> lines() { return lines; }
}
