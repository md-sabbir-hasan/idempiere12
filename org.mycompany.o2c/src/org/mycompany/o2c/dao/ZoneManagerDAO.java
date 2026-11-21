package org.mycompany.o2c.dao;

import java.sql.Timestamp;

import org.mycompany.o2c.model.IdName;
import org.mycompany.o2c.model.ZoneManager;
import org.mycompany.o2c.util.DBUtils;

/**
 * Finds the Zone Manager of an order: the Zone is the parent (Sales Region tree, level name "Zone")
 * of the order's Territory (Base); the Zone Manager is the officer assigned to that Zone in
 * XX_SalesRegionOfficer (Officer Assignment form), valid TODAY (approval happens now, not on the order date).
 */
public class ZoneManagerDAO {

    /** Level no of the Zone in the Sales Region tree (1 National, 2 Division, 3 Zone, 4 Base). */
    public static final int ZONE_LEVEL_NO = 3;

    /** The Zone above the territory (walks up the Sales Region tree); null when there is none. */
    public IdName findZone(int clientId, int territoryId) {
        return DBUtils.queryOne(
            "WITH RECURSIVE up(id, depth) AS ( "
          + " SELECT CAST(? AS numeric), 0 "
          + " UNION ALL "
          + " SELECT n.parent_id, up.depth + 1 FROM up "
          + " JOIN ad_treenode n ON n.node_id = up.id AND n.ad_tree_id = "
          + "   (SELECT MIN(t.ad_tree_id) FROM ad_tree t WHERE t.ad_client_id = ? "
          + "    AND t.treetype = 'SR' AND t.isactive = 'Y') "
          + " WHERE n.parent_id IS NOT NULL AND n.parent_id <> 0 AND up.depth < 10) "
          + "SELECT r.c_salesregion_id, r.name FROM up "
          + "JOIN c_salesregion r ON r.c_salesregion_id = up.id "
          + "WHERE COALESCE(r.xx_levelno, 0) = " + ZONE_LEVEL_NO + " ORDER BY up.depth LIMIT 1",
            rs -> new IdName(rs.getInt(1), rs.getString(2)),
            Integer.valueOf(territoryId), Integer.valueOf(clientId));
    }

    /**
     * Zone and Zone Manager for the territory. Null when no Zone is found above it;
     * userId is 0 when the Zone has no current Zone Manager.
     * Only an assignment for the order's Sales Type AND Segment counts (no other assignment of the Zone).
     * date: null = today. Approval callers pass null.
     */
    public ZoneManager find(int clientId, int territoryId, int docTypeId, int segmentId, Timestamp date) {
        IdName zone = findZone(clientId, territoryId);
        if (zone == null)
            return null;
        Timestamp on = date != null ? date
                : Timestamp.valueOf(java.time.LocalDate.now().atStartOfDay());   // today, End Date is a whole day
        IdName mgr = DBUtils.queryOne(
            "SELECT o.ad_user_id, u.name FROM xx_salesregionofficer o "
          + "JOIN ad_user u ON u.ad_user_id = o.ad_user_id "
          + "WHERE o.ad_client_id = ? AND o.isactive = 'Y' AND o.c_salesregion_id = ? "
          + "AND o.startdate <= ? AND (o.enddate IS NULL OR o.enddate >= ?) "
          + "AND o.c_doctype_id = ? AND o.c_activity_id = ? "
          + "ORDER BY o.startdate DESC LIMIT 1",
            rs -> new IdName(rs.getInt(1), rs.getString(2)),
            Integer.valueOf(clientId), Integer.valueOf(zone.id()), on, on,
            Integer.valueOf(docTypeId), Integer.valueOf(segmentId));
        return new ZoneManager(zone.id(), zone.name(), mgr == null ? 0 : mgr.id(),
                mgr == null ? "" : mgr.name());
    }
}
