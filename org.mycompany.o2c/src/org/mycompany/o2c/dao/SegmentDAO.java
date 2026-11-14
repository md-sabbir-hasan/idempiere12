package org.mycompany.o2c.dao;

import java.util.List;

import org.mycompany.o2c.model.Segment;
import org.mycompany.o2c.util.DBUtils;

/** Database access for XX_Segment / XX_BPartnerSegment. */
public class SegmentDAO {

    /** Active segments assigned to the customer (XX_BPartnerSegment). */
    public List<Segment> findByCustomer(int bpartnerId) {
        return DBUtils.queryList(
            "SELECT s.XX_Segment_ID, s.Name FROM XX_Segment s "
          + "JOIN XX_BPartnerSegment bs ON bs.XX_Segment_ID=s.XX_Segment_ID "
          + "WHERE bs.C_BPartner_ID=? AND bs.IsActive='Y' AND s.IsActive='Y' "
          + "AND s.AD_Client_ID=? ORDER BY s.Name",
            rs -> new Segment(rs.getInt(1), rs.getString(2)),
            bpartnerId, DBUtils.clientId());
    }
}
