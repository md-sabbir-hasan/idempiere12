package org.mycompany.o2c.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Read only rows of the Officer Audit dialog. */
public final class AuditRows {
    private AuditRows() {
    }

    /** A sales order the officer created or is sales rep of. edited = changed after it was created. */
    public record Order(int id, String documentNo, Timestamp dateOrdered, String customer, String region,
            String segment, String docStatus, BigDecimal grandTotal, Timestamp created, String createdBy,
            Timestamp updated, String updatedBy, boolean edited) {
    }

    /** One Change Log entry (only columns that have Allow Logging). */
    public record Change(Timestamp when, String who, String what, String column, String oldValue,
            String newValue) {
    }

    /** One login session of the officer. */
    public record Session(Timestamp login, Timestamp lastSeen, String ip, String role, boolean closed) {
    }

    /** An approval step handled by the user (Zone Manager / Sales Admin). */
    public record Approval(Timestamp created, String orderNo, String step, String state, String assignedTo,
            String doneBy, String message) {
    }
}
