package org.mycompany.o2c.model;

import java.sql.Timestamp;

/** One row of XX_SalesRegionOfficer. endDate == null means the officer is still assigned. */
public record OfficerAssignment(int id, int regionId, String regionName,
        int officerId, String officerName,
        int docTypeId, String docTypeName,
        int activityId, String activityName,
        Timestamp startDate, Timestamp endDate) {

    public boolean isOpen() {
        return endDate == null;
    }
}
