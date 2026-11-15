package org.mycompany.o2c.service;

import java.sql.Timestamp;
import java.util.List;

import org.mycompany.o2c.dao.HeaderLookupDAO;
import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.util.ValidationUtils;

public class HeaderLookupService {
    private final HeaderLookupDAO dao = new HeaderLookupDAO();

    /** Depot of the territory on the date. Null when no territory / date, or no depot is assigned. */
    public Option getDepot(int territoryId, Timestamp date) {
        if (!ValidationUtils.isId(territoryId) || date == null)
            return null;
        return dao.findDepot(territoryId, date);
    }

    public List<Option> getLocations() {
        return dao.findElementValues(HeaderLookupDAO.ELEMENT_LOCATION);
    }

    public List<Option> getDepartments() {
        return dao.findElementValues(HeaderLookupDAO.ELEMENT_DEPARTMENT);
    }

    public int getCurrentUserId() {
        return dao.currentUserId();
    }
}
