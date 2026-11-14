package org.mycompany.o2c.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.mycompany.o2c.dao.SalesOrderDAO;
import org.mycompany.o2c.model.OrderTotals;
import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.model.Product;
import org.mycompany.o2c.model.SalesOrderLine;
import org.mycompany.o2c.util.ValidationException;
import org.mycompany.o2c.util.ValidationUtils;

/** Order level business logic: line validation, amounts and totals. */
public class SalesOrderService {
    private final SalesOrderDAO dao = new SalesOrderDAO();
    private final ProductService productService;
    private final TaxService taxService;
    private final DiscountService discountService;

    public SalesOrderService() {
        this(new ProductService(), new TaxService(), new DiscountService());
    }

    public SalesOrderService(ProductService productService, TaxService taxService,
            DiscountService discountService) {
        this.productService = productService;
        this.taxService = taxService;
        this.discountService = discountService;
    }

    public List<Option> getOrderTypes() {
        return dao.findOrderTypes();
    }

    /**
     * Validates the selection and builds a calculated order line.
     *
     * @throws ValidationException with a user friendly message when something is wrong
     */
    public SalesOrderLine buildLine(int bpartnerId, int warehouseId, int segmentId, String segmentName,
            int categoryId, String categoryName, int productId, Product product, BigDecimal qty,
            List<SalesOrderLine> existing) {

        ValidationUtils.requireId(bpartnerId, "Select a customer first.");
        ValidationUtils.requireId(warehouseId, "Select a Depot / Warehouse.");
        if (segmentId <= 0 || categoryId <= 0 || productId <= 0)
            throw new ValidationException("Select Segment, Category and Product.");
        ValidationUtils.requirePositive(qty, "Enter a quantity greater than zero.");
        if (product == null || product.getPrice() == null)
            throw new ValidationException("This product has no price in the Price List.");

        if (!productService.isValidSelection(bpartnerId, segmentId, categoryId, productId))
            throw new ValidationException("Invalid Segment / Category / Product combination for this customer.");

        for (SalesOrderLine l : existing) {
            if (l.getProductId() == productId)
                throw new ValidationException("This product is already in the order. Edit or remove that line.");
        }

        SalesOrderLine l = new SalesOrderLine();
        l.setSegmentId(segmentId);
        l.setCategoryId(categoryId);
        l.setProductId(productId);
        l.setSegmentName(segmentName);
        l.setCategoryName(categoryName);
        l.setCode(product.getCode());
        l.setName(product.getName());
        l.setDescription(product.getDescription());
        l.setUom(product.getUom());
        l.setQty(qty);
        l.setPrice(product.getPrice());
        l.setTaxRate(product.getTaxRate());
        calculate(l);
        return l;
    }

    /** Fills gross, discount, vat and total of the line. */
    public void calculate(SalesOrderLine line) {
        BigDecimal gross = line.getQty().multiply(line.getPrice()).setScale(2, RoundingMode.HALF_UP);
        line.setGross(gross);
        line.setDiscount(discountService.calculateDiscount(line));
        line.setVat(taxService.vatOnGross(gross, line.getTaxRate()));
        line.setTotal(gross.subtract(line.getDiscount()).add(line.getVat()));
    }

    public OrderTotals calculateTotals(List<SalesOrderLine> lines) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (SalesOrderLine l : lines) {
            gross = gross.add(l.getGross());
            discount = discount.add(l.getDiscount());
            vat = vat.add(l.getVat());
        }
        return new OrderTotals(gross, discount, vat, gross.subtract(discount).add(vat));
    }
}
