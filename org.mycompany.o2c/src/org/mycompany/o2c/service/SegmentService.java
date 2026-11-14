package org.mycompany.o2c.service;

import java.util.Collections;
import java.util.List;

import org.mycompany.o2c.dao.SegmentDAO;
import org.mycompany.o2c.model.Segment;
import org.mycompany.o2c.util.ValidationUtils;

public class SegmentService {
    private final SegmentDAO dao = new SegmentDAO();

    /** Segments the customer may buy from. Empty when no customer is selected or none is assigned. */
    public List<Segment> getCustomerSegments(int bpartnerId) {
        if (!ValidationUtils.isId(bpartnerId))
            return Collections.emptyList();
        return dao.findByCustomer(bpartnerId);
    }
}
