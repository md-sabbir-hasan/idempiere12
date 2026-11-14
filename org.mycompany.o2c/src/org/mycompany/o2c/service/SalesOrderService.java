package org.mycompany.o2c.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.mycompany.o2c.dao.OrderPersistenceDAO;
import org.mycompany.o2c.dao.SalesOrderDAO;
import org.mycompany.o2c.model.OrderHeader;
import org.mycompany.o2c.model.OrderTotals;
import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.model.Product;
import org.mycompany.o2c.model.SalesOrderLine;
import org.mycompany.o2c.model.SavedOrder;
import org.mycompany.o2c.util.ValidationException;
import org.mycompany.o2c.util.ValidationUtils;

/** Order level business logic: line validation, amounts and totals. */
public class SalesOrderService {
    private final SalesOrderDAO dao = new SalesOrderDAO();
    private final OrderPersistenceDAO persistence = new OrderPersistenceDAO();
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

    public List<Option> getPriceLists() {
        return dao.findSalesPriceLists();
    }

    public List<Option> getSalesReps() {
        return dao.findSalesReps();
    }

    public List<Option> getPaymentTerms() {
        return dao.findPaymentTerms();
    }

    public int getCustomerPaymentTerm(int bpartnerId) {
        return dao.findCustomerPaymentTerm(bpartnerId);
    }

    /**
     * Validates the order and stores it as a Draft (MOrder / MOrderLine, one transaction).
     * The order is not completed, nothing is reserved, shipped, invoiced or paid.
     *
     * @throws ValidationException with a user friendly message when something is missing
     */
    public SavedOrder saveDraft(OrderHeader h, List<SalesOrderLine> lines) {
        ValidationUtils.requireId(h.getOrgId(), "Select an Organization.");
        ValidationUtils.requireId(h.getBpartnerId(), "Select a customer.");
        ValidationUtils.requireId(h.getWarehouseId(), "Select a Depot / Warehouse.");
        ValidationUtils.requireId(h.getDocTypeId(), "Select the Type of Order.");
        ValidationUtils.requireId(h.getPriceListId(), "Select a Price List.");
        ValidationUtils.requireNotNull(h.getDateOrdered(), "Enter the Order Date.");
        if (h.getDatePromised() != null && h.getDatePromised().before(h.getDateOrdered()))
            throw new ValidationException("Expected Delivery cannot be before the Order Date.");
        if (h.isSiteDelivery() && h.getSiteAddress().trim().isEmpty())
            throw new ValidationException("Enter the site delivery address.");
        if (lines == null || lines.isEmpty())
            throw new ValidationException("Add at least one product line.");
        return persistence.saveDraft(h, lines);
    }

    /**
     * Validates the selection and builds a calculated order line.
     *
     * @throws ValidationException with a user friendly message when something is wrong
     */
    public SalesOrderLine buildLine(int bpartnerId, int warehouseId, int segmentId, String segmentName,
            int categoryId, String categoryName, int productId, Product product, BigDecimal qty,
            List<SalesOrderLine> existing) {
        return buildLine(bpartnerId, warehouseId, segmentId, segmentName, categoryId, categoryName,
                productId, product, qty, BigDecimal.ZERO, existing);
    }

    public SalesOrderLine buildLine(int bpartnerId, int warehouseId, int segmentId, String segmentName,
            int categoryId, String categoryName, int productId, Product product, BigDecimal qty,
            BigDecimal specialDiscountPct, List<SalesOrderLine> existing) {

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
        l.setSpecialDiscountPct(discountService.validateSpecialDiscountPct(specialDiscountPct));
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
