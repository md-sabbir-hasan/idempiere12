package org.mycompany.o2c.dao;

import java.sql.Timestamp;
import java.util.List;

import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.mycompany.o2c.model.AuditRows.Approval;
import org.mycompany.o2c.model.AuditRows.Change;
import org.mycompany.o2c.model.AuditRows.Order;
import org.mycompany.o2c.model.AuditRows.Session;
import org.mycompany.o2c.util.DBUtils;

/** Read only queries of the Officer Audit: what a user did in a period. Nothing is written here. */
public class AuditDAO {

    /** Sales orders the user created or is Sales Rep of, in the period (by creation date). activityId 0 = all Segments. */
    public List<Order> findOrders(int userId, int activityId, Timestamp from, Timestamp toExclusive) {
        return DBUtils.queryList(
            "SELECT o.c_order_id, o.documentno, o.dateordered, bp.name, COALESCE(r.name, ''), "
          + "COALESCE(a.name, ''), o.docstatus, o.grandtotal, o.created, COALESCE(cu.name, ''), "
          + "o.updated, COALESCE(uu.name, ''), (o.updated > o.created + interval '1 minute') "
          + "FROM c_order o JOIN c_bpartner bp ON bp.c_bpartner_id = o.c_bpartner_id "
          + "LEFT JOIN c_salesregion r ON r.c_salesregion_id = o.c_salesregion_id "
          + "LEFT JOIN c_activity a ON a.c_activity_id = o.c_activity_id "
          + "LEFT JOIN ad_user cu ON cu.ad_user_id = o.createdby "
          + "LEFT JOIN ad_user uu ON uu.ad_user_id = o.updatedby "
          + "WHERE o.ad_client_id = ? AND o.issotrx = 'Y' AND (o.createdby = ? OR o.salesrep_id = ?) "
          + "AND o.created >= ? AND o.created < ? AND (? = 0 OR o.c_activity_id = ?) "
          + "ORDER BY o.created DESC LIMIT 1000",
            rs -> new Order(rs.getInt(1), rs.getString(2), rs.getTimestamp(3), rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getString(7), rs.getBigDecimal(8), rs.getTimestamp(9), rs.getString(10),
                    rs.getTimestamp(11), rs.getString(12), rs.getBoolean(13)),
            Integer.valueOf(DBUtils.clientId()), Integer.valueOf(userId), Integer.valueOf(userId), from,
            toExclusive, Integer.valueOf(activityId), Integer.valueOf(activityId));
    }

    /** Change Log of the order and its lines (only columns with Allow Logging; empty when logging was off). */
    public List<Change> findChanges(int orderId) {
        return DBUtils.queryList(
            "SELECT cl.created, COALESCE(u.name, ''), "
          + "CASE WHEN cl.ad_table_id = ? THEN 'Order' ELSE 'Line' END, c.columnname, "
          + "COALESCE(cl.oldvalue, ''), COALESCE(cl.newvalue, '') "
          + "FROM ad_changelog cl JOIN ad_column c ON c.ad_column_id = cl.ad_column_id "
          + "LEFT JOIN ad_user u ON u.ad_user_id = cl.createdby "
          + "WHERE (cl.ad_table_id = ? AND cl.record_id = ?) "
          + "OR (cl.ad_table_id = ? AND cl.record_id IN "
          + "     (SELECT c_orderline_id FROM c_orderline WHERE c_order_id = ?)) "
          + "ORDER BY cl.created, cl.ad_changelog_id",
            rs -> new Change(rs.getTimestamp(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getString(6)),
            Integer.valueOf(MOrder.Table_ID), Integer.valueOf(MOrder.Table_ID), Integer.valueOf(orderId),
            Integer.valueOf(MOrderLine.Table_ID), Integer.valueOf(orderId));
    }

    /** Login sessions of the user in the period. */
    public List<Session> findSessions(int userId, Timestamp from, Timestamp toExclusive) {
        return DBUtils.queryList(
            "SELECT s.created, s.updated, COALESCE(s.remote_addr, ''), COALESCE(r.name, ''), "
          + "(s.processed = 'Y') "
          + "FROM ad_session s LEFT JOIN ad_role r ON r.ad_role_id = s.ad_role_id "
          + "WHERE s.createdby = ? AND s.created >= ? AND s.created < ? ORDER BY s.created DESC LIMIT 500",
            rs -> new Session(rs.getTimestamp(1), rs.getTimestamp(2), rs.getString(3), rs.getString(4),
                    rs.getBoolean(5)),
            Integer.valueOf(userId), from, toExclusive);
    }

    /** Approval steps (ZM / Sales Admin) that were given to the user or done by him in the period. */
    public List<Approval> findApprovals(int userId, Timestamp from, Timestamp toExclusive) {
        return DBUtils.queryList(
            "SELECT a.created, o.documentno, n.name, a.wfstate, COALESCE(au.name, ''), "
          + "CASE WHEN a.processed = 'Y' THEN COALESCE(uu.name, '') ELSE '' END, COALESCE(a.textmsg, '') "
          + "FROM ad_wf_activity a JOIN ad_wf_node n ON n.ad_wf_node_id = a.ad_wf_node_id "
          + "JOIN c_order o ON o.c_order_id = a.record_id "
          + "LEFT JOIN ad_user au ON au.ad_user_id = a.ad_user_id "
          + "LEFT JOIN ad_user uu ON uu.ad_user_id = a.updatedby "
          + "WHERE a.ad_table_id = ? AND n.name IN (?, ?) AND (a.ad_user_id = ? OR a.updatedby = ?) "
          + "AND a.created >= ? AND a.created < ? ORDER BY a.created DESC LIMIT 1000",
            rs -> new Approval(rs.getTimestamp(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getString(6), rs.getString(7)),
            Integer.valueOf(MOrder.Table_ID), ApprovalDAO.ZM_NODE, ApprovalDAO.ADMIN_NODE,
            Integer.valueOf(userId), Integer.valueOf(userId), from, toExclusive);
    }
}
