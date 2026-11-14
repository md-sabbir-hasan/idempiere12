package org.mycompany.o2c.dao;

import java.math.BigDecimal;

import org.mycompany.o2c.util.DBUtils;

/**
 * Read only access to iDempiere storage. Inventory stays owned by iDempiere:
 * nothing here (or anywhere in this plugin) writes to the storage tables.
 */
public class InventoryDAO {

    public BigDecimal getOnHand(int productId, int warehouseId) {
        BigDecimal v = DBUtils.queryBigDecimal(
            "SELECT COALESCE(SUM(oh.QtyOnHand),0) FROM M_StorageOnHand oh "
          + "JOIN M_Locator l ON l.M_Locator_ID=oh.M_Locator_ID "
          + "WHERE oh.M_Product_ID=? AND l.M_Warehouse_ID=?",
            productId, warehouseId);
        return v == null ? BigDecimal.ZERO : v;
    }

    public BigDecimal getReserved(int productId, int warehouseId) {
        BigDecimal v = DBUtils.queryBigDecimal(
            "SELECT COALESCE(SUM(Qty),0) FROM M_StorageReservation "
          + "WHERE M_Product_ID=? AND M_Warehouse_ID=? AND IsSOTrx='Y'",
            productId, warehouseId);
        return v == null ? BigDecimal.ZERO : v;
    }
}
