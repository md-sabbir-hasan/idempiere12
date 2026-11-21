package org.mycompany.o2c.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MBPartner;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.mycompany.o2c.model.OrderHeader;
import org.mycompany.o2c.model.SalesOrderLine;
import org.mycompany.o2c.model.SavedOrder;
import org.mycompany.o2c.util.ValidationException;

/**
 * Stores the order through the standard iDempiere models (MOrder / MOrderLine) inside ONE
 * transaction. Nothing is committed unless the header and every line were saved.
 * The order stays Drafted: no DocAction (complete / prepare), no reservation, no shipment/invoice.
 * Submitting for approval is a separate step: see ApprovalDAO.
 */
public class OrderPersistenceDAO {

    // custom columns that must be registered in the Application Dictionary
    // (Segment = C_Activity_ID and Sales Type = C_DocTypeTarget_ID are standard columns;
    //  C_SalesRegion_ID = Territory must be added to C_Order, see db/02_default_tables.sql;
    //  XX_SpecialDiscount is used by the approval workflow, see db/03_approval.sql)
    private static final String[] HEADER_XX = { "C_SalesRegion_ID",
            "XX_IsSiteDelivery", "XX_SiteDeliveryAddress", "XX_PaymentType", "XX_SpecialDiscount" };

    /** Stores a new Draft. */
    public SavedOrder saveDraft(OrderHeader h, List<SalesOrderLine> lines) {
        return store(0, h, lines);
    }

    /** Replaces header and lines of an existing Draft of the same officer. */
    public SavedOrder updateDraft(int orderId, OrderHeader h, List<SalesOrderLine> lines) {
        return store(orderId, h, lines);
    }

    private SavedOrder store(int orderId, OrderHeader h, List<SalesOrderLine> lines) {
        Properties ctx = Env.getCtx();
        String trxName = Trx.createTrxName("O2CDraft");
        Trx trx = Trx.get(trxName, true);
        try {
            MBPartner bp = new MBPartner(ctx, h.getBpartnerId(), trxName);

            MOrder order = new MOrder(ctx, orderId, trxName);
            if (orderId > 0) {
                if (order.get_ID() != orderId)
                    throw new ValidationException("The order was not found.");
                if (!MOrder.DOCSTATUS_Drafted.equals(order.getDocStatus()))
                    throw new ValidationException("Only a Draft order can be changed. This order is "
                            + order.getDocStatus() + ".");
                if (order.getSalesRep_ID() != h.getSalesRepId())
                    throw new ValidationException("This order belongs to another officer.");
            }
            requireColumns(order, HEADER_XX, "C_Order");

            order.setAD_Org_ID(h.getOrgId());
            order.setIsSOTrx(true);                       // before setBPartner (see javadoc)
            order.setC_DocTypeTarget_ID(h.getDocTypeId());
            order.setBPartner(bp);                        // sets bill / ship location defaults
            if (order.getC_BPartner_Location_ID() <= 0)
                throw new ValidationException("The customer has no address (Location). Add one first.");
            order.setM_Warehouse_ID(h.getWarehouseId());
            order.setM_PriceList_ID(h.getPriceListId());  // also sets the currency
            if (h.getPaymentTermId() > 0)
                order.setC_PaymentTerm_ID(h.getPaymentTermId());
            if (h.getSalesRepId() > 0)
                order.setSalesRep_ID(h.getSalesRepId());
            order.setDateOrdered(h.getDateOrdered());
            order.setDatePromised(h.getDatePromised() != null ? h.getDatePromised() : h.getDateOrdered());
            order.setDescription(h.getDescription());

            order.set_ValueOfColumn("User1_ID", Integer.valueOf(h.getLocationId()));    // Location
            order.set_ValueOfColumn("User2_ID", Integer.valueOf(h.getDepartmentId()));  // Department
            order.set_ValueOfColumn("XX_PaymentType", h.getPaymentType());
            order.setC_Activity_ID(h.getSegmentId());                                  // Segment
            if (h.getTerritoryId() > 0)
                order.set_ValueOfColumn("C_SalesRegion_ID", Integer.valueOf(h.getTerritoryId())); // Territory
            order.set_ValueOfColumn("XX_IsSiteDelivery", Boolean.valueOf(h.isSiteDelivery()));
            order.set_ValueOfColumn("XX_SiteDeliveryAddress", h.isSiteDelivery() ? h.getSiteAddress() : null);

            // total special discount of the order: the approval workflow looks at this one column
            BigDecimal special = BigDecimal.ZERO;
            for (SalesOrderLine l : lines)
                if (l.getDiscount() != null)
                    special = special.add(l.getDiscount());
            order.set_ValueOfColumn("XX_SpecialDiscount", special);

            order.saveEx();

            if (orderId > 0) {
                for (MOrderLine old : order.getLines(true, null))
                    old.deleteEx(true);
            }

            for (SalesOrderLine l : lines) {
                MOrderLine ol = new MOrderLine(order);
                ol.setM_Product_ID(l.getProductId(), true);   // also takes the product UOM
                ol.setQtyEntered(l.getQty());
                ol.setQtyOrdered(l.getQty());
                ol.setPrice();                                // MProductPricing on the order price list
                if (ol.getPriceActual() == null || ol.getPriceActual().signum() <= 0)
                    throw new ValidationException("No price in the selected Price List for: " + l.getName());
                if (!ol.setTax())
                    throw new ValidationException("No tax could be determined for: " + l.getName());
                if (l.getDiscount() != null && l.getDiscount().signum() > 0) {
                    // keep PriceList, lower the price actually charged: iDempiere then shows the
                    // special discount as the line Discount %. net = gross - special discount
                    BigDecimal net = l.getGross().subtract(l.getDiscount());
                    BigDecimal unit = net.divide(l.getQty(), 6, java.math.RoundingMode.HALF_UP);
                    ol.setPriceEntered(unit);
                    ol.setPriceActual(unit);
                }
                ol.setC_Activity_ID(l.getSegmentId());                    // Segment
                ol.saveEx();
            }

            // re-read the header: line saves update the header totals
            MOrder saved = new MOrder(ctx, order.getC_Order_ID(), trxName);
            SavedOrder result = new SavedOrder(saved.getC_Order_ID(), saved.getDocumentNo(),
                    saved.getDocStatus(), saved.getTotalLines(), saved.getGrandTotal());

            trx.commit(true);
            return result;
        } catch (ValidationException ex) {
            trx.rollback();
            throw ex;
        } catch (Exception ex) {
            trx.rollback();
            throw new RuntimeException("Save failed, nothing was stored: " + ex.getMessage(), ex);
        } finally {
            trx.close();
        }
    }

    /** Fails early, with a readable message, when a custom column is not registered in the AD. */
    private static void requireColumns(PO po, String[] columns, String table) {
        StringBuilder missing = new StringBuilder();
        for (String c : columns) {
            if (po.get_ColumnIndex(c) < 0) {
                if (missing.length() > 0)
                    missing.append(", ");
                missing.append(c);
            }
        }
        if (missing.length() > 0)
            throw new ValidationException("Register these columns in the Application Dictionary ("
                    + table + "): " + missing);
    }
}
