package org.mycompany.o2c.service;

import java.util.Collections;
import java.util.List;

import org.mycompany.o2c.dao.WarehouseDAO;
import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.model.Warehouse;
import org.mycompany.o2c.util.ValidationUtils;

public class WarehouseService {
    private final WarehouseDAO dao = new WarehouseDAO();

    public List<Option> getOrganizations() {
        return dao.findOrganizations();
    }

    /** Warehouses of the organization. Empty when no organization is selected. */
    public List<Warehouse> getWarehouses(int orgId) {
        if (!ValidationUtils.isId(orgId))
            return Collections.emptyList();
        return dao.findByOrganization(orgId);
    }
}
