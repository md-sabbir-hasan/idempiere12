package org.mycompany.o2c.service;

import java.util.Collections;
import java.util.List;

import org.mycompany.o2c.dao.CustomerDAO;
import org.mycompany.o2c.model.Customer;
import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.util.ValidationUtils;

public class CustomerService {
    public static final String LOCAL = "L";
    public static final String FOREIGN = "F";

    private final CustomerDAO dao = new CustomerDAO();

    private static boolean isSalesType(String st) {
        return LOCAL.equals(st) || FOREIGN.equals(st);
    }

    public List<Option> getTerritories(String salesType) {
        if (!isSalesType(salesType))
            return Collections.emptyList();
        return dao.findTerritories(salesType);
    }

    /** Customers for the sales type; territoryId 0 = National (all territories of that sales type). */
    public List<Customer> getCustomers(int territoryId, String salesType) {
        if (!isSalesType(salesType))
            return Collections.emptyList();
        return dao.findCustomers(Math.max(territoryId, 0), salesType);
    }

    /** Full customer record, or null when no customer is selected / found. */
    public Customer getCustomerDetails(int bpartnerId) {
        if (!ValidationUtils.isId(bpartnerId))
            return null;
        return dao.findDetails(bpartnerId);
    }
}
