package org.mycompany.o2c.dao;

import java.util.List;

import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.util.DBUtils;

/**
 * Database access for the sales order header lookups.
 * Saving / completing the order will go through MOrder (not SQL) and is added in a later stage.
 */
public class SalesOrderDAO {

    /** Sales order document types (DocBaseType SOO). */
    public List<Option> findOrderTypes() {
        return DBUtils.queryList(
            "SELECT C_DocType_ID, Name FROM C_DocType WHERE IsActive='Y' "
          + "AND AD_Client_ID=? AND DocBaseType='SOO' ORDER BY Name",
            rs -> new Option(rs.getInt(1), rs.getString(2)),
            DBUtils.clientId());
    }
}
