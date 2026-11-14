package org.mycompany.o2c.model;

import java.math.BigDecimal;

/**
 * Customer (C_BPartner). Customers returned by a list query only carry id and name;
 * {@code CustomerService.getCustomerDetails} returns the full record.
 */
public class Customer {
    private final int id;
    private final String name;

    private String taxId = "";
    private BigDecimal creditLimit = BigDecimal.ZERO;
    private String paymentTerm = "";
    private int creditDays;
    private BigDecimal vatRate = BigDecimal.ZERO;
    private String address = "";
    private String contactName = "";
    private String contactPhone = "";

    public Customer(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public String getPaymentTerm() {
        return paymentTerm;
    }

    public void setPaymentTerm(String paymentTerm) {
        this.paymentTerm = paymentTerm;
    }

    public int getCreditDays() {
        return creditDays;
    }

    public void setCreditDays(int creditDays) {
        this.creditDays = creditDays;
    }

    /** Customer wise VAT rate (XX_VATRate), display only. Real VAT comes from TaxService. */
    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}
