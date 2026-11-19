package org.mycompany.o2c.dao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.mycompany.o2c.model.OrderRow;
import org.mycompany.o2c.util.DBUtils;

/** Read only query for the order list (C_Order of the logged in sales representative). */
public class OrderListDAO {

    /**
     * Sales orders of the officer, newest first (at most 500).
     * from: first day included (null = no limit), toExclusive: day after the last day (null = no limit),
     * customerLike: part of the customer name ("" = all), docStatus: "DR", "CO"... ("" = all).
     */
    public List<OrderRow> find(int officerId, Timestamp from, Timestamp toExclusive,
            String customerLike, String docStatus) {
        StringBuilder sql = new StringBuilder(
            "SELECT o.c_order_id, o.documentno, o.dateordered, bp.name, COALESCE(r.name, ''), "
          + "COALESCE(d.name, ''), COALESCE(a.name, ''), o.grandtotal, o.docstatus "
          + "FROM c_order o JOIN c_bpartner bp ON bp.c_bpartner_id = o.c_bpartner_id "
          + "LEFT JOIN c_salesregion r ON r.c_salesregion_id = o.c_salesregion_id "
          + "LEFT JOIN c_doctype d ON d.c_doctype_id = o.c_doctypetarget_id "
          + "LEFT JOIN c_activity a ON a.c_activity_id = o.c_activity_id "
          + "WHERE o.ad_client_id = ? AND o.issotrx = 'Y' AND o.isactive = 'Y' AND o.salesrep_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(Integer.valueOf(DBUtils.clientId()));
        params.add(Integer.valueOf(officerId));
        if (from != null) {
            sql.append(" AND o.dateordered >= ?");
            params.add(from);
        }
        if (toExclusive != null) {
            sql.append(" AND o.dateordered < ?");
            params.add(toExclusive);
        }
        if (customerLike != null && !customerLike.trim().isEmpty()) {
            sql.append(" AND lower(bp.name) LIKE ?");
            params.add("%" + customerLike.trim().toLowerCase() + "%");
        }
        if (docStatus != null && !docStatus.isEmpty()) {
            sql.append(" AND o.docstatus = ?");
            params.add(docStatus);
        }
        sql.append(" ORDER BY o.dateordered DESC, o.c_order_id DESC LIMIT 500");
        return DBUtils.queryList(sql.toString(), rs -> new OrderRow(rs.getInt(1), rs.getString(2),
                rs.getTimestamp(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
                rs.getBigDecimal(8), rs.getString(9)), params.toArray());
    }
}
