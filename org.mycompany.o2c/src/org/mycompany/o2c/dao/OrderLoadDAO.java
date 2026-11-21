package org.mycompany.o2c.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.mycompany.o2c.model.OrderDetail;
import org.mycompany.o2c.model.OrderHeader;
import org.mycompany.o2c.model.SalesOrderLine;
import org.mycompany.o2c.util.DBUtils;

/** Read only: one stored order (header and lines) of an officer, to show it again on the form. */
public class OrderLoadDAO {

    /** The order when it belongs to the officer; null otherwise. Amounts are recalculated by the service. */
    public OrderDetail find(int orderId, int officerId) {
        OrderDetail d = DBUtils.queryOne(
            "SELECT o.c_order_id, o.documentno, o.docstatus, o.ad_org_id, COALESCE(o.m_warehouse_id,0), "
          + "COALESCE(o.c_doctypetarget_id,0), o.c_bpartner_id, COALESCE(o.m_pricelist_id,0), "
          + "COALESCE(o.c_paymentterm_id,0), o.dateordered, o.datepromised, COALESCE(o.description,''), "
          + "COALESCE(o.c_activity_id,0), COALESCE(o.c_salesregion_id,0), "
          + "COALESCE(o.xx_issitedelivery,'N'), COALESCE(o.xx_sitedeliveryaddress,''), "
          + "COALESCE(o.user1_id,0), COALESCE(o.user2_id,0), COALESCE(o.xx_paymenttype,''), "
          + "COALESCE(o.salesrep_id,0) "
          + "FROM c_order o WHERE o.c_order_id = ? AND o.ad_client_id = ? AND o.issotrx = 'Y' "
          + "AND o.isactive = 'Y' AND o.salesrep_id = ?",
            rs -> {
                OrderDetail x = new OrderDetail();
                x.setOrderId(rs.getInt(1));
                x.setDocumentNo(rs.getString(2));
                x.setDocStatus(rs.getString(3));
                OrderHeader h = new OrderHeader();
                h.setOrgId(rs.getInt(4));
                h.setWarehouseId(rs.getInt(5));
                h.setDocTypeId(rs.getInt(6));
                h.setBpartnerId(rs.getInt(7));
                h.setPriceListId(rs.getInt(8));
                h.setPaymentTermId(rs.getInt(9));
                h.setDateOrdered(rs.getTimestamp(10));
                h.setDatePromised(rs.getTimestamp(11));
                h.setDescription(rs.getString(12));
                h.setSegmentId(rs.getInt(13));
                h.setTerritoryId(rs.getInt(14));
                h.setSiteDelivery("Y".equals(rs.getString(15)));
                h.setSiteAddress(rs.getString(16));
                h.setLocationId(rs.getInt(17));
                h.setDepartmentId(rs.getInt(18));
                h.setPaymentType(rs.getString(19));
                h.setSalesRepId(rs.getInt(20));
                x.setHeader(h);
                return x;
            },
            Integer.valueOf(orderId), Integer.valueOf(DBUtils.clientId()), Integer.valueOf(officerId));
        if (d == null)
            return null;

        List<SalesOrderLine> found = DBUtils.queryList(
            "SELECT p.m_product_category_id, COALESCE(pc.name,''), p.m_product_id, p.value, p.name, "
          + "COALESCE(p.description,''), COALESCE(u.uomsymbol, u.name), ol.qtyordered, "
          + "COALESCE(NULLIF(ol.pricelist,0), ol.priceactual), COALESCE(t.rate,0), ol.linenetamt, "
          + "COALESCE(ol.c_activity_id,0), COALESCE(a.name,'') "
          + "FROM c_orderline ol JOIN m_product p ON p.m_product_id = ol.m_product_id "
          + "LEFT JOIN m_product_category pc ON pc.m_product_category_id = p.m_product_category_id "
          + "JOIN c_uom u ON u.c_uom_id = p.c_uom_id "
          + "LEFT JOIN c_tax t ON t.c_tax_id = ol.c_tax_id "
          + "LEFT JOIN c_activity a ON a.c_activity_id = ol.c_activity_id "
          + "WHERE ol.c_order_id = ? AND ol.isactive = 'Y' ORDER BY ol.line, ol.c_orderline_id",
            rs -> {
                SalesOrderLine l = new SalesOrderLine();
                l.setCategoryId(rs.getInt(1));
                l.setCategoryName(rs.getString(2));
                l.setProductId(rs.getInt(3));
                l.setCode(rs.getString(4));
                l.setName(rs.getString(5));
                l.setDescription(rs.getString(6));
                l.setUom(rs.getString(7));
                BigDecimal qty = rs.getBigDecimal(8);
                BigDecimal price = rs.getBigDecimal(9);
                l.setQty(qty);
                l.setPrice(price);
                l.setTaxRate(rs.getBigDecimal(10));
                l.setSegmentId(rs.getInt(12));
                l.setSegmentName(rs.getString(13));
                // the special discount was stored by lowering the price: read it back as a percent
                BigDecimal gross = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
                BigDecimal off = gross.subtract(rs.getBigDecimal(11));
                if (gross.signum() > 0 && off.signum() > 0)
                    l.setSpecialDiscountPct(off.multiply(new BigDecimal("100"))
                            .divide(gross, 6, RoundingMode.HALF_UP));
                return l;
            },
            Integer.valueOf(orderId));
        d.lines().addAll(found);
        return d;
    }
}
