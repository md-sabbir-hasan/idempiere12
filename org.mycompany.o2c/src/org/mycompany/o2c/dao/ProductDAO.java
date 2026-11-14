package org.mycompany.o2c.dao;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.mycompany.o2c.model.Product;
import org.mycompany.o2c.util.DBUtils;

/** Database access for M_Product and its Price List price. */
public class ProductDAO {

    /** Sold products of the category, name = "code - name". */
    public List<Product> findByCategory(int categoryId) {
        return DBUtils.queryList(
            "SELECT M_Product_ID, Value || ' - ' || Name FROM M_Product "
          + "WHERE M_Product_Category_ID=? AND IsActive='Y' AND IsSold='Y' AND IsSummary='N' "
          + "AND AD_Client_ID=? ORDER BY Name",
            rs -> new Product(rs.getInt(1), rs.getString(2)),
            categoryId, DBUtils.clientId());
    }

    /** Product details; null when the product is not in the given category. */
    public Product findInCategory(int productId, int categoryId) {
        return DBUtils.queryOne(
            "SELECT p.M_Product_ID, p.Value, p.Name, COALESCE(p.Description,''), "
          + "COALESCE(u.UOMSymbol,u.Name), COALESCE(p.C_TaxCategory_ID,0) FROM M_Product p "
          + "JOIN C_UOM u ON u.C_UOM_ID=p.C_UOM_ID "
          + "WHERE p.M_Product_ID=? AND p.M_Product_Category_ID=?",
            rs -> {
                Product p = new Product(rs.getInt(1), rs.getString(3));
                p.setCategoryId(categoryId);
                p.setCode(rs.getString(2));
                p.setDescription(rs.getString(4));
                p.setUom(rs.getString(5));
                p.setTaxCategoryId(rs.getInt(6));
                return p;
            },
            productId, categoryId);
    }

    /**
     * Standard price from a sales Price List version valid on the date.
     * The customer's own Price List is preferred, otherwise the newest valid version.
     * Null when no price exists.
     */
    public BigDecimal findPrice(int productId, int bpartnerId, Timestamp date) {
        return DBUtils.queryBigDecimal(
            "SELECT pp.PriceStd FROM M_ProductPrice pp "
          + "JOIN M_PriceList_Version plv ON plv.M_PriceList_Version_ID=pp.M_PriceList_Version_ID "
          + "JOIN M_PriceList pl ON pl.M_PriceList_ID=plv.M_PriceList_ID "
          + "WHERE pp.M_Product_ID=? AND pp.IsActive='Y' AND pl.IsSOPriceList='Y' "
          + "AND pl.IsActive='Y' AND pl.AD_Client_ID=? AND plv.IsActive='Y' AND plv.ValidFrom<=? "
          + "ORDER BY CASE WHEN pl.M_PriceList_ID=(SELECT M_PriceList_ID FROM C_BPartner "
          + "WHERE C_BPartner_ID=?) THEN 0 ELSE 1 END, plv.ValidFrom DESC LIMIT 1",
            productId, DBUtils.clientId(), date, bpartnerId);
    }

    /** True when customer -> segment -> category -> product is a configured combination. */
    public boolean isAllowedForCustomer(int productId, int segmentId, int categoryId, int bpartnerId) {
        String ok = DBUtils.queryString(
            "SELECT 1 FROM M_Product p "
          + "JOIN XX_SegmentCategory sc ON sc.M_Product_Category_ID=p.M_Product_Category_ID "
          + "AND sc.IsActive='Y' "
          + "JOIN XX_BPartnerSegment bs ON bs.XX_Segment_ID=sc.XX_Segment_ID AND bs.IsActive='Y' "
          + "WHERE p.M_Product_ID=? AND sc.XX_Segment_ID=? AND sc.M_Product_Category_ID=? "
          + "AND bs.C_BPartner_ID=? AND p.AD_Client_ID=? LIMIT 1",
            productId, segmentId, categoryId, bpartnerId, DBUtils.clientId());
        return !ok.isEmpty();
    }
}
