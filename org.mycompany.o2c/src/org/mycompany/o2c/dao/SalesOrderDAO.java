package org.mycompany.o2c.dao;

import java.util.List;

import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.util.DBUtils;

/**
 * Database access for the sales order header lookups.
 * Storing the order is in {@link OrderPersistenceDAO} (MOrder / MOrderLine, not SQL).
 */
public class SalesOrderDAO {

    /** Sales order document types (DocBaseType SOO). */
    public List<Option> findOrderTypes() {
        return DBUtils.queryList(
            "SELECT C_DocType_ID, Name FROM C_DocType WHERE IsActive='Y' "
          + "AND AD_Client_ID=? AND DocBaseType='SOO' ORDER BY Name",
            rs -> new Option(rs.getInt(1), rs.getString(2)),
            DBUtils.clientId());
    }

    /** Sales price lists with their currency, e.g. "Sales Price (BDT)". */
    public List<Option> findSalesPriceLists() {
        return DBUtils.queryList(
            "SELECT pl.M_PriceList_ID, pl.Name || ' (' || c.ISO_Code || ')' FROM M_PriceList pl "
          + "JOIN C_Currency c ON c.C_Currency_ID=pl.C_Currency_ID "
          + "WHERE pl.IsActive='Y' AND pl.IsSOPriceList='Y' AND pl.AD_Client_ID=? ORDER BY pl.Name",
            rs -> new Option(rs.getInt(1), rs.getString(2)),
            DBUtils.clientId());
    }

    /** Sales representatives (users whose business partner is flagged Sales Rep). */
    public List<Option> findSalesReps() {
        return DBUtils.queryList(
            "SELECT u.AD_User_ID, u.Name FROM AD_User u "
          + "JOIN C_BPartner bp ON bp.C_BPartner_ID=u.C_BPartner_ID "
          + "WHERE u.IsActive='Y' AND bp.IsSalesRep='Y' AND u.AD_Client_ID=? ORDER BY u.Name",
            rs -> new Option(rs.getInt(1), rs.getString(2)),
            DBUtils.clientId());
    }

    public List<Option> findPaymentTerms() {
        return DBUtils.queryList(
            "SELECT C_PaymentTerm_ID, Name FROM C_PaymentTerm WHERE IsActive='Y' "
          + "AND AD_Client_ID=? ORDER BY Name",
            rs -> new Option(rs.getInt(1), rs.getString(2)),
            DBUtils.clientId());
    }

    /** Payment term set on the customer (0 when none). */
    public int findCustomerPaymentTerm(int bpartnerId) {
        Integer v = DBUtils.queryOne("SELECT C_PaymentTerm_ID FROM C_BPartner WHERE C_BPartner_ID=?",
            rs -> Integer.valueOf(rs.getInt(1)), bpartnerId);
        return v == null ? 0 : v.intValue();
    }
}
