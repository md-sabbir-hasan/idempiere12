package org.mycompany.o2c.dao;

import java.sql.Timestamp;
import java.util.List;

import org.compiere.util.Env;
import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.util.DBUtils;

/** Header lookups of the Sales Order form: Depot of a territory, Location, Department. */
public class HeaderLookupDAO {

    /** Name of the user-defined account elements (Account Element window). */
    public static final String ELEMENT_LOCATION = "Primitek Group Location";
    public static final String ELEMENT_DEPARTMENT = "Primitek Group Department";

    /**
     * Depot (warehouse) of the territory on the order date (XX_TerritoryDepot, Date Start / Date Finish).
     * A territory without its own row uses the nearest parent territory that has one
     * (Dhaka North uses the depot of Dhaka). Depth is limited because a root territory may point to itself.
     */
    public Option findDepot(int territoryId, Timestamp date) {
        return DBUtils.queryOne(
            "WITH RECURSIVE chain(xx_territory_id, parent_id, lvl) AS ("
          + "  SELECT xx_territory_id, parent_id, 0 FROM XX_Territory WHERE xx_territory_id=? "
          + "  UNION ALL "
          + "  SELECT t.xx_territory_id, t.parent_id, c.lvl+1 FROM XX_Territory t "
          + "  JOIN chain c ON t.xx_territory_id=c.parent_id WHERE c.lvl<6) "
          + "SELECT w.M_Warehouse_ID, w.Name FROM chain c "
          + "JOIN XX_TerritoryDepot d ON d.xx_territory_id=c.xx_territory_id "
          + "JOIN M_Warehouse w ON w.M_Warehouse_ID=d.M_Warehouse_ID "
          + "WHERE d.IsActive='Y' AND w.IsActive='Y' "
          + "AND CAST(d.DateStart AS DATE) <= CAST(? AS DATE) "
          + "AND (d.DateFinish IS NULL OR CAST(d.DateFinish AS DATE) >= CAST(? AS DATE)) "
          + "ORDER BY c.lvl, d.DateStart DESC",
            rs -> new Option(rs.getInt(1), rs.getString(2)),
            territoryId, date, date);
    }

    /** Active values of a user-defined account element, by element name. */
    public List<Option> findElementValues(String elementName) {
        return DBUtils.queryList(
            "SELECT v.C_ElementValue_ID, v.Name FROM C_ElementValue v "
          + "JOIN C_Element e ON e.C_Element_ID=v.C_Element_ID "
          + "WHERE e.Name=? AND e.AD_Client_ID=? AND v.IsActive='Y' ORDER BY v.Name",
            rs -> new Option(rs.getInt(1), rs.getString(2)),
            elementName, DBUtils.clientId());
    }

    /** AD_User_ID of the logged in user. */
    public int currentUserId() {
        return Env.getAD_User_ID(Env.getCtx());
    }
}
