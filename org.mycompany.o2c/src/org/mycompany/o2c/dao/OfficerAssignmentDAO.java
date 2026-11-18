package org.mycompany.o2c.dao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.mycompany.o2c.model.AssignmentFilter.Status;
import org.mycompany.o2c.model.IdName;
import org.mycompany.o2c.model.OfficerAssignment;
import org.mycompany.o2c.model.RegionNode;
import org.mycompany.o2c.util.DBUtils;
import org.mycompany.o2c.util.ValidationException;

/** SQL only. Table: XX_SalesRegionOfficer (assignment history, End Date empty = current officer). */
public class OfficerAssignmentDAO {

    /** Text shown for an officer: "2636-Md. Mostak Ahmed" (partner code - user name). */
    private static final String OFFICER_LABEL =
        "CASE WHEN bp.value IS NULL THEN u.name ELSE bp.value || '-' || u.name END";

    private static Boolean hasUuColumn;

    public List<IdName> findSalesTypes() {
        return DBUtils.queryList(
            "SELECT c_doctype_id, name FROM c_doctype WHERE ad_client_id = ? AND isactive = 'Y' "
          + "AND issotrx = 'Y' AND docbasetype = 'SOO' AND docsubtypeso = 'SO' ORDER BY name",
            rs -> new IdName(rs.getInt(1), rs.getString(2)), DBUtils.clientId());
    }

    public List<IdName> findSegments() {
        return DBUtils.queryList(
            "SELECT c_activity_id, name FROM c_activity WHERE ad_client_id = ? AND isactive = 'Y' "
          + "AND issummary = 'N' ORDER BY name",
            rs -> new IdName(rs.getInt(1), rs.getString(2)), DBUtils.clientId());
    }

    /** Users that can be an officer: users of a Sales Representative business partner. */
    public List<IdName> findOfficers() {
        return DBUtils.queryList(
            "SELECT u.ad_user_id, " + OFFICER_LABEL + " AS label "
          + "FROM ad_user u JOIN c_bpartner bp ON bp.c_bpartner_id = u.c_bpartner_id "
          + "WHERE u.ad_client_id = ? AND u.isactive = 'Y' AND bp.isactive = 'Y' AND bp.issalesrep = 'Y' "
          + "ORDER BY 2",
            rs -> new IdName(rs.getInt(1), rs.getString(2)), DBUtils.clientId());
    }

    /** All active regions with their parent from the Sales Region tree (tree type SR). */
    public List<RegionNode> findRegions() {
        return DBUtils.queryList(
            "SELECT r.c_salesregion_id, r.name, COALESCE(n.parent_id, 0), "
          + "COALESCE(r.xx_levelno, 0), COALESCE(r.xx_levelname, '') "
          + "FROM c_salesregion r "
          + "LEFT JOIN ad_treenode n ON n.node_id = r.c_salesregion_id AND n.ad_tree_id = "
          + "  (SELECT MIN(t.ad_tree_id) FROM ad_tree t WHERE t.ad_client_id = r.ad_client_id "
          + "   AND t.treetype = 'SR' AND t.isactive = 'Y') "
          + "WHERE r.ad_client_id = ? AND r.isactive = 'Y' ORDER BY r.name",
            rs -> new RegionNode(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getString(5)),
            DBUtils.clientId());
    }

    /** 0 for docTypeId / activityId / officerId = no filter. */
    public List<OfficerAssignment> findAssignments(int docTypeId, int activityId, int officerId, Status status) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        if (docTypeId > 0) {
            where.append(" AND o.c_doctype_id = ?");
            params.add(Integer.valueOf(docTypeId));
        }
        if (activityId > 0) {
            where.append(" AND o.c_activity_id = ?");
            params.add(Integer.valueOf(activityId));
        }
        if (officerId > 0) {
            where.append(" AND o.ad_user_id = ?");
            params.add(Integer.valueOf(officerId));
        }
        if (status == Status.ACTIVE)
            where.append(" AND o.enddate IS NULL");
        else if (status == Status.ENDED)
            where.append(" AND o.enddate IS NOT NULL");
        return queryAssignments(where.toString(), params);
    }

    public OfficerAssignment findById(int id) {
        List<Object> params = new ArrayList<Object>();
        params.add(Integer.valueOf(id));
        List<OfficerAssignment> list = queryAssignments(" AND o.xx_salesregionofficer_id = ?", params);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<OfficerAssignment> queryAssignments(String extraWhere, List<Object> extraParams) {
        String sql =
            "SELECT o.xx_salesregionofficer_id, o.c_salesregion_id, r.name, o.ad_user_id, "
          + OFFICER_LABEL + ", o.c_doctype_id, d.name, o.c_activity_id, a.name, o.startdate, o.enddate "
          + "FROM xx_salesregionofficer o "
          + "JOIN c_salesregion r ON r.c_salesregion_id = o.c_salesregion_id "
          + "JOIN ad_user u ON u.ad_user_id = o.ad_user_id "
          + "LEFT JOIN c_bpartner bp ON bp.c_bpartner_id = u.c_bpartner_id "
          + "JOIN c_doctype d ON d.c_doctype_id = o.c_doctype_id "
          + "JOIN c_activity a ON a.c_activity_id = o.c_activity_id "
          + "WHERE o.ad_client_id = ? AND o.isactive = 'Y'" + extraWhere
          + " ORDER BY o.startdate DESC, r.name";
        List<Object> params = new ArrayList<Object>();
        params.add(Integer.valueOf(DBUtils.clientId()));
        params.addAll(extraParams);
        return DBUtils.queryList(sql, rs -> new OfficerAssignment(
                rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getString(5),
                rs.getInt(6), rs.getString(7), rs.getInt(8), rs.getString(9),
                rs.getTimestamp(10), rs.getTimestamp(11)),
            params.toArray());
    }

    /**
     * One transaction: first ends the old assignments (endIds, with endDate), then inserts one
     * assignment per region. Everything is saved, or nothing.
     */
    public void assign(int officerId, int docTypeId, int activityId, Timestamp startDate,
            List<Integer> regionIds, List<Integer> endIds, Timestamp endDate) {
        int clientId = DBUtils.clientId();
        int orgId = Env.getAD_Org_ID(Env.getCtx());
        int userId = Env.getAD_User_ID(Env.getCtx());
        boolean uu = hasUuColumn();
        String trxName = Trx.createTrxName("OfficerAssign");
        Trx trx = Trx.get(trxName, true);
        try {
            for (Integer oldId : endIds) {
                DB.executeUpdateEx("UPDATE xx_salesregionofficer SET enddate = ?, updated = now(), "
                    + "updatedby = ? WHERE xx_salesregionofficer_id = ? AND enddate IS NULL",
                    new Object[] { endDate, Integer.valueOf(userId), oldId }, trxName);
            }
            String insert = "INSERT INTO xx_salesregionofficer (xx_salesregionofficer_id, ad_client_id, "
                + "ad_org_id, isactive, created, createdby, updated, updatedby, c_salesregion_id, "
                + "ad_user_id, c_doctype_id, c_activity_id, startdate"
                + (uu ? ", xx_salesregionofficer_uu" : "") + ") "
                + "VALUES (?, ?, ?, 'Y', now(), ?, now(), ?, ?, ?, ?, ?, ?"
                + (uu ? ", gen_random_uuid()::varchar" : "") + ")";
            for (Integer regionId : regionIds) {
                int id = DB.getNextID(clientId, "XX_SalesRegionOfficer", trxName);
                DB.executeUpdateEx(insert, new Object[] { Integer.valueOf(id), Integer.valueOf(clientId),
                    Integer.valueOf(orgId), Integer.valueOf(userId), Integer.valueOf(userId), regionId,
                    Integer.valueOf(officerId), Integer.valueOf(docTypeId), Integer.valueOf(activityId),
                    startDate }, trxName);
            }
            trx.commit(true);
        } catch (Exception ex) {
            trx.rollback();
            String m = String.valueOf(ex.getMessage());
            if (m.contains("xx_sro_open_uk"))
                throw new ValidationException(
                    "Someone just assigned an officer to one of these regions. Refresh and try again.");
            throw new RuntimeException("Save failed, nothing was stored: " + m, ex);
        } finally {
            trx.close();
        }
    }

    /** Sets the End Date of a current assignment. Returns false when it was already ended. */
    public boolean end(int assignmentId, Timestamp endDate) {
        int rows = DB.executeUpdateEx("UPDATE xx_salesregionofficer SET enddate = ?, updated = now(), "
            + "updatedby = ? WHERE xx_salesregionofficer_id = ? AND enddate IS NULL",
            new Object[] { endDate, Integer.valueOf(Env.getAD_User_ID(Env.getCtx())),
                Integer.valueOf(assignmentId) }, null);
        return rows > 0;
    }

    /** The UU column exists only if it was part of the CREATE TABLE script. */
    private static synchronized boolean hasUuColumn() {
        if (hasUuColumn == null) {
            hasUuColumn = Boolean.valueOf(!DBUtils.queryString(
                "SELECT column_name FROM information_schema.columns "
              + "WHERE table_name = 'xx_salesregionofficer' AND column_name = 'xx_salesregionofficer_uu'")
                .isEmpty());
        }
        return hasUuColumn.booleanValue();
    }
}
