package org.mycompany.o2c.dao;

import java.math.BigDecimal;
import java.util.List;

import org.mycompany.o2c.model.Customer;
import org.mycompany.o2c.util.DBUtils;

/** Database access for customers (C_BPartner). */
public class CustomerDAO {

    /**
     * Active customers of a Territory: the customer has an active Location whose
     * Sales Region (C_BPartner_Location.C_SalesRegion_ID) is the Territory.
     */
    public List<Customer> findCustomers(int territoryId) {
        return DBUtils.queryList(
            "SELECT bp.C_BPartner_ID, bp.Name FROM C_BPartner bp "
          + "WHERE bp.IsActive='Y' AND bp.IsCustomer='Y' AND bp.AD_Client_ID=? "
          + "AND EXISTS (SELECT 1 FROM C_BPartner_Location bl WHERE bl.C_BPartner_ID=bp.C_BPartner_ID "
          + "AND bl.IsActive='Y' AND bl.C_SalesRegion_ID=?) ORDER BY bp.Name",
            rs -> new Customer(rs.getInt(1), rs.getString(2)),
            DBUtils.clientId(), territoryId);
    }

    /** Full customer record for the information card; null when the customer is not found. */
    public Customer findDetails(int bpartnerId) {
        Customer c = DBUtils.queryOne(
            "SELECT bp.C_BPartner_ID, bp.Name, COALESCE(bp.TaxID,''), "
          + "COALESCE(bp.SO_CreditLimit,0), COALESCE(pt.Name,''), "
          + "COALESCE(bp.XX_CreditDays, pt.NetDays, 0), COALESCE(bp.XX_VATRate,0) "
          + "FROM C_BPartner bp LEFT JOIN C_PaymentTerm pt "
          + "ON pt.C_PaymentTerm_ID=bp.C_PaymentTerm_ID "
          + "WHERE bp.C_BPartner_ID=? AND bp.AD_Client_ID=?",
            rs -> {
                Customer x = new Customer(rs.getInt(1), rs.getString(2));
                x.setTaxId(rs.getString(3));
                x.setCreditLimit(rs.getBigDecimal(4));
                x.setPaymentTerm(rs.getString(5));
                BigDecimal days = rs.getBigDecimal(6);
                x.setCreditDays(days == null ? 0 : days.intValue());
                x.setVatRate(rs.getBigDecimal(7));
                return x;
            },
            bpartnerId, DBUtils.clientId());
        if (c == null)
            return null;

        c.setAddress(DBUtils.queryString(
            "SELECT COALESCE(l.Address1,'') || CASE WHEN l.City IS NULL THEN '' ELSE ', ' || l.City END "
          + "FROM C_BPartner_Location bl JOIN C_Location l ON l.C_Location_ID=bl.C_Location_ID "
          + "WHERE bl.C_BPartner_ID=? AND bl.IsActive='Y' ORDER BY bl.C_BPartner_Location_ID LIMIT 1",
            bpartnerId));

        String[] contact = DBUtils.queryOne(
            "SELECT COALESCE(Name,''), COALESCE(Phone,'') FROM AD_User "
          + "WHERE C_BPartner_ID=? AND IsActive='Y' ORDER BY AD_User_ID LIMIT 1",
            rs -> new String[] { rs.getString(1), rs.getString(2) },
            bpartnerId);
        if (contact != null) {
            c.setContactName(contact[0]);
            c.setContactPhone(contact[1]);
        }
        return c;
    }
}
