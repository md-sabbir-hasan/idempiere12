package org.mycompany.o2c.dao;

import java.util.List;

import org.mycompany.o2c.model.Category;
import org.mycompany.o2c.util.DBUtils;

/** Database access for M_Product_Category through XX_SegmentCategory. */
public class CategoryDAO {

    /** Active categories mapped to the segment. */
    public List<Category> findBySegment(int segmentId) {
        return DBUtils.queryList(
            "SELECT pc.M_Product_Category_ID, pc.Name FROM M_Product_Category pc "
          + "JOIN XX_SegmentCategory sc ON sc.M_Product_Category_ID=pc.M_Product_Category_ID "
          + "WHERE sc.XX_Segment_ID=? AND sc.IsActive='Y' AND pc.IsActive='Y' ORDER BY pc.Name",
            rs -> new Category(rs.getInt(1), rs.getString(2)),
            segmentId);
    }
}
