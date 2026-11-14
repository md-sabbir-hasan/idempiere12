package org.mycompany.o2c.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.mycompany.o2c.model.Customer;
import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.util.DBUtils;

/** Database access for customers (C_BPartner) and territories (XX_Territory). */
public class CustomerDAO {

    /** Active territories of the sales type (L = Local, F = Foreign). */
    public List<Option> findTerritories(String salesType) {
        return DBUtils.queryList(
            "SELECT t.XX_Territory_ID, t.Name FROM XX_Territory t "
          + "WHERE t.IsActive='Y' AND t.AD_Client_ID=? AND t.SalesType=? ORDER BY t.Name",
            rs -> new Option(rs.getInt(1), rs.getString(2)),
            DBUtils.clientId(), salesType);
    }

    /**
     * Active customers. A territory filters directly; territoryId = 0 means "National":
     * every customer whose territory has the given sales type.
     */
    public List<Customer> findCustomers(int territoryId, String salesType) {
        StringBuilder sql = new StringBuilder(
            "SELECT bp.C_BPartner_ID, bp.Name FROM C_BPartner bp "
          + "WHERE bp.IsActive='Y' AND bp.IsCustomer='Y' AND bp.AD_Client_ID=? ");
        List<Object> params = new ArrayList<Object>();
        params.add(DBUtils.clientId());

        if (territoryId > 0) {
            sql.append("AND bp.XX_Territory_ID=? ");
            params.add(territoryId);
        } else {
            sql.append("AND EXISTS (SELECT 1 FROM XX_Territory t "
                     + "  WHERE t.XX_Territory_ID=bp.XX_Territory_ID AND t.SalesType=?) ");
            params.add(salesType);
        }
        sql.append("ORDER BY bp.Name");

        return DBUtils.queryList(sql.toString(),
            rs -> new Customer(rs.getInt(1), rs.getString(2)),
            params.toArray());
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
