package org.mycompany.o2c.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.mycompany.o2c.dao.ProductDAO;
import org.mycompany.o2c.model.Product;
import org.mycompany.o2c.util.ValidationUtils;

public class ProductService {
    private final ProductDAO dao = new ProductDAO();
    private final TaxService taxService = new TaxService();

    /** Sold products of the category. Empty when no category is selected. */
    public List<Product> getCategoryProducts(int categoryId) {
        if (!ValidationUtils.isId(categoryId))
            return Collections.emptyList();
        return dao.findByCategory(categoryId);
    }

    /**
     * Product with price and tax rate. Null when the product does not belong to the category
     * (so a product of another category can never be loaded by accident).
     */
    public Product getProductInfo(int productId, int categoryId, int bpartnerId, Date orderDate) {
        if (!ValidationUtils.isId(productId) || !ValidationUtils.isId(categoryId))
            return null;
        Product p = dao.findInCategory(productId, categoryId);
        if (p == null)
            return null;

        if (p.getDescription() == null || p.getDescription().isEmpty())
            p.setDescription(p.getName());

        p.setPrice(findPrice(productId, bpartnerId, orderDate));
        p.setTaxRate(taxService.getRate(p.getTaxCategoryId()));
        return p;
    }

    /** Price List price on the order date; null when the product has no price. */
    public BigDecimal findPrice(int productId, int bpartnerId, Date orderDate) {
        Timestamp ts = new Timestamp(orderDate == null ? System.currentTimeMillis() : orderDate.getTime());
        return dao.findPrice(productId, bpartnerId, ts);
    }

    /** True when the customer is allowed to buy this product through the segment and category. */
    public boolean isValidSelection(int bpartnerId, int segmentId, int categoryId, int productId) {
        return dao.isAllowedForCustomer(productId, segmentId, categoryId, bpartnerId);
    }
}
