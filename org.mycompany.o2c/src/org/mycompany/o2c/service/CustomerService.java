package org.mycompany.o2c.service;

import java.util.Collections;
import java.util.List;

import org.mycompany.o2c.dao.CustomerDAO;
import org.mycompany.o2c.model.Customer;
import org.mycompany.o2c.util.ValidationUtils;

public class CustomerService {
    private final CustomerDAO dao = new CustomerDAO();

    /** Customers of the Territory (Sales Region). Empty when no Territory is selected. */
    public List<Customer> getCustomers(int territoryId) {
        if (!ValidationUtils.isId(territoryId))
            return Collections.emptyList();
        return dao.findCustomers(territoryId);
    }

    /** Full customer record, or null when no customer is selected / found. */
    public Customer getCustomerDetails(int bpartnerId) {
        if (!ValidationUtils.isId(bpartnerId))
            return null;
        return dao.findDetails(bpartnerId);
    }
}
