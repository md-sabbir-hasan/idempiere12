package org.mycompany.o2c.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.mycompany.o2c.dao.AuditDAO;
import org.mycompany.o2c.model.AuditRows.Approval;
import org.mycompany.o2c.model.AuditRows.Change;
import org.mycompany.o2c.model.AuditRows.Order;
import org.mycompany.o2c.model.AuditRows.Session;
import org.mycompany.o2c.util.ValidationException;

/** Officer Audit: read only. The last date of the period is included. */
public class AuditService {
    private final AuditDAO dao = new AuditDAO();

    private static void check(LocalDate from, LocalDate to) {
        if (from == null || to == null)
            throw new ValidationException("Enter the From and To dates.");
        if (to.isBefore(from))
            throw new ValidationException("The 'To' date is before the 'From' date.");
    }

    private static Timestamp start(LocalDate d) {
        return Timestamp.valueOf(d.atStartOfDay());
    }

    private static Timestamp endExclusive(LocalDate d) {
        return Timestamp.valueOf(d.plusDays(1).atStartOfDay());
    }

    public List<Order> orders(int userId, int activityId, LocalDate from, LocalDate to) {
        check(from, to);
        return dao.findOrders(userId, activityId, start(from), endExclusive(to));
    }

    public List<Change> changes(int orderId) {
        return dao.findChanges(orderId);
    }

    public List<Session> sessions(int userId, LocalDate from, LocalDate to) {
        check(from, to);
        return dao.findSessions(userId, start(from), endExclusive(to));
    }

    public List<Approval> approvals(int userId, LocalDate from, LocalDate to) {
        check(from, to);
        return dao.findApprovals(userId, start(from), endExclusive(to));
    }
}
