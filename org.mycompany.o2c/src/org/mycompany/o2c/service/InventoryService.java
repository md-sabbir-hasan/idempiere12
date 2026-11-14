package org.mycompany.o2c.service;

import org.mycompany.o2c.dao.InventoryDAO;
import org.mycompany.o2c.model.StockInfo;
import org.mycompany.o2c.util.ValidationUtils;

public class InventoryService {
    private final InventoryDAO dao = new InventoryDAO();

    /** Stock of the product in the warehouse; {@link StockInfo#EMPTY} when either is not selected. */
    public StockInfo getStock(int productId, int warehouseId) {
        if (!ValidationUtils.isId(productId) || !ValidationUtils.isId(warehouseId))
            return StockInfo.EMPTY;
        return new StockInfo(dao.getOnHand(productId, warehouseId),
                             dao.getReserved(productId, warehouseId));
    }
}
