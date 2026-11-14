package org.mycompany.o2c.dao;

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
 */
public class OrderPersistenceDAO {

    // custom columns that must be registered in the Application Dictionary
    private static final String[] HEADER_XX = { "XX_SalesType", "XX_Territory_ID",
            "XX_IsSiteDelivery", "XX_SiteDeliveryAddress" };
    private static final String[] LINE_XX = { "XX_Segment_ID" };

    public SavedOrder saveDraft(OrderHeader h, List<SalesOrderLine> lines) {
        Properties ctx = Env.getCtx();
        String trxName = Trx.createTrxName("O2CDraft");
        Trx trx = Trx.get(trxName, true);
        try {
            MBPartner bp = new MBPartner(ctx, h.getBpartnerId(), trxName);

            MOrder order = new MOrder(ctx, 0, trxName);
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

            order.set_ValueOfColumn("XX_SalesType", h.getSalesType());
            if (h.getTerritoryId() > 0)
                order.set_ValueOfColumn("XX_Territory_ID", Integer.valueOf(h.getTerritoryId()));
            order.set_ValueOfColumn("XX_IsSiteDelivery", Boolean.valueOf(h.isSiteDelivery()));
            if (h.isSiteDelivery())
                order.set_ValueOfColumn("XX_SiteDeliveryAddress", h.getSiteAddress());

            order.saveEx();

            for (SalesOrderLine l : lines) {
                MOrderLine ol = new MOrderLine(order);
                requireColumns(ol, LINE_XX, "C_OrderLine");
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
                    java.math.BigDecimal net = l.getGross().subtract(l.getDiscount());
                    java.math.BigDecimal unit = net.divide(l.getQty(), 6, java.math.RoundingMode.HALF_UP);
                    ol.setPriceEntered(unit);
                    ol.setPriceActual(unit);
                }
                ol.set_ValueOfColumn("XX_Segment_ID", Integer.valueOf(l.getSegmentId()));
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
