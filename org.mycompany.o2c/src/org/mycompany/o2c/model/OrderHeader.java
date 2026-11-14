package org.mycompany.o2c.model;

import java.sql.Timestamp;

/** Header values entered on the form, passed to the service when the order is saved. */
public class OrderHeader {
    private int orgId;
    private int warehouseId;
    private int docTypeId;
    private int bpartnerId;
    private int priceListId;
    private int salesRepId;
    private int paymentTermId;
    private Timestamp dateOrdered;
    private Timestamp datePromised;
    private String description = "";
    private String salesType = "";
    private int territoryId;
    private boolean siteDelivery;
    private String siteAddress = "";

    public int getOrgId() { return orgId; }
    public void setOrgId(int v) { orgId = v; }
    public int getWarehouseId() { return warehouseId; }
    public void setWarehouseId(int v) { warehouseId = v; }
    public int getDocTypeId() { return docTypeId; }
    public void setDocTypeId(int v) { docTypeId = v; }
    public int getBpartnerId() { return bpartnerId; }
    public void setBpartnerId(int v) { bpartnerId = v; }
    public int getPriceListId() { return priceListId; }
    public void setPriceListId(int v) { priceListId = v; }
    public int getSalesRepId() { return salesRepId; }
    public void setSalesRepId(int v) { salesRepId = v; }
    public int getPaymentTermId() { return paymentTermId; }
    public void setPaymentTermId(int v) { paymentTermId = v; }
    public Timestamp getDateOrdered() { return dateOrdered; }
    public void setDateOrdered(Timestamp v) { dateOrdered = v; }
    public Timestamp getDatePromised() { return datePromised; }
    public void setDatePromised(Timestamp v) { datePromised = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v == null ? "" : v; }
    public String getSalesType() { return salesType; }
    public void setSalesType(String v) { salesType = v == null ? "" : v; }
    public int getTerritoryId() { return territoryId; }
    public void setTerritoryId(int v) { territoryId = v; }
    public boolean isSiteDelivery() { return siteDelivery; }
    public void setSiteDelivery(boolean v) { siteDelivery = v; }
    public String getSiteAddress() { return siteAddress; }
    public void setSiteAddress(String v) { siteAddress = v == null ? "" : v; }
}
