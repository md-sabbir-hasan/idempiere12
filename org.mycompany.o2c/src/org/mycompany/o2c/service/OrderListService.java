package org.mycompany.o2c.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.mycompany.o2c.dao.OrderListDAO;
import org.mycompany.o2c.model.OrderRow;
import org.mycompany.o2c.util.ValidationException;

/** The order list of the logged in sales representative. */
public class OrderListService {
    private final OrderListDAO dao = new OrderListDAO();

    /** Dates may be null (no limit). The last date is included. */
    public List<OrderRow> list(int officerId, LocalDate from, LocalDate to, String customer, String docStatus) {
        if (from != null && to != null && to.isBefore(from))
            throw new ValidationException("The 'To' date is before the 'From' date.");
        Timestamp f = from == null ? null : Timestamp.valueOf(from.atStartOfDay());
        Timestamp t = to == null ? null : Timestamp.valueOf(to.plusDays(1).atStartOfDay());
        return dao.find(officerId, f, t, customer, docStatus);
    }
}
