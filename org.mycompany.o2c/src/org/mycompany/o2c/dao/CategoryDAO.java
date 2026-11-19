package org.mycompany.o2c.dao;

import java.util.List;

import org.mycompany.o2c.model.Category;
import org.mycompany.o2c.util.DBUtils;

/** Database access for M_Product_Category. A category belongs to a Segment (C_Activity_ID). */
public class CategoryDAO {

    /** Active categories of the segment. */
    public List<Category> findBySegment(int segmentId) {
        return DBUtils.queryList(
            "SELECT pc.M_Product_Category_ID, pc.Name FROM M_Product_Category pc "
          + "WHERE pc.C_Activity_ID=? AND pc.IsActive='Y' AND pc.AD_Client_ID=? ORDER BY pc.Name",
            rs -> new Category(rs.getInt(1), rs.getString(2)),
            segmentId, DBUtils.clientId());
    }
}
