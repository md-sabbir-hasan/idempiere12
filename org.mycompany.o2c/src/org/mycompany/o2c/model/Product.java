package org.mycompany.o2c.model;

import java.math.BigDecimal;

/**
 * Product (M_Product). List queries only carry id and name (name = "code - name").
 * {@code ProductService.getProductInfo} returns the full record with price and tax rate.
 */
public class Product {
    private final int id;
    private final String name;

    private int categoryId;
    private String code = "";
    private String description = "";
    private String uom = "";
    private int taxCategoryId;
    private BigDecimal price;
    private BigDecimal taxRate = BigDecimal.ZERO;

    public Product(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public int getTaxCategoryId() {
        return taxCategoryId;
    }

    public void setTaxCategoryId(int taxCategoryId) {
        this.taxCategoryId = taxCategoryId;
    }

    /** Price List price, or null when the product has no price. */
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
}
