package org.mycompany.o2c.dao;

import java.util.List;

import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.model.Warehouse;
import org.mycompany.o2c.util.DBUtils;

/** Database access for organizations (AD_Org) and warehouses (M_Warehouse). */
public class WarehouseDAO {

    /** Active, non-summary organizations of the tenant (excluding the * organization). */
    public List<Option> findOrganizations() {
        return DBUtils.queryList(
            "SELECT AD_Org_ID, Name FROM AD_Org WHERE IsActive='Y' AND IsSummary='N' "
          + "AND AD_Client_ID=? AND AD_Org_ID>0 ORDER BY Name",
            rs -> new Option(rs.getInt(1), rs.getString(2)),
            DBUtils.clientId());
    }

    /** Active warehouses of the organization. */
    public List<Warehouse> findByOrganization(int orgId) {
        return DBUtils.queryList(
            "SELECT M_Warehouse_ID, Name FROM M_Warehouse WHERE IsActive='Y' "
          + "AND AD_Client_ID=? AND AD_Org_ID=? ORDER BY Name",
            rs -> new Warehouse(rs.getInt(1), rs.getString(2)),
            DBUtils.clientId(), orgId);
    }
}
