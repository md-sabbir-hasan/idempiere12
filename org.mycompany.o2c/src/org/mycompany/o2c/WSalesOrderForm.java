//package org.mycompany.o2c;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.Timestamp;
//import java.text.DecimalFormat;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.adempiere.webui.component.Button;
//import org.adempiere.webui.component.Label;
//import org.adempiere.webui.panel.ADForm;
//import org.adempiere.webui.util.ZKUpdateUtil;
//import org.compiere.util.DB;
//import org.compiere.util.Env;
//import org.zkoss.zk.ui.Component;
//import org.zkoss.zk.ui.event.Event;
//import org.zkoss.zk.ui.event.EventListener;
//import org.zkoss.zk.ui.event.Events;
//import org.zkoss.zul.Borderlayout;
//import org.zkoss.zul.Center;
//import org.zkoss.zul.Checkbox;
//import org.zkoss.zul.Datebox;
//import org.zkoss.zul.Decimalbox;
//import org.zkoss.zul.Div;
//import org.zkoss.zul.Listbox;
//import org.zkoss.zul.Listcell;
//import org.zkoss.zul.Listhead;
//import org.zkoss.zul.Listheader;
//import org.zkoss.zul.Listitem;
//import org.zkoss.zul.Messagebox;
//import org.zkoss.zul.North;
//import org.zkoss.zul.Textbox;
//
///**
// * Sales Order form.
// * Stage 1: layout, Segment -> Category -> Product cascade, product details,
// * price, warehouse stock, order lines (add / edit / remove), totals.
// * Not yet: product attributes, discount / Buy X Get Y, save / complete.
// */
//public class WSalesOrderForm extends ADForm implements EventListener<Event> {
//    private static final long serialVersionUID = 1L;
//
//    private static final String CARD =
//        "background:#ffffff; border:1px solid #e2e8f0; border-radius:12px; "
//      + "box-shadow:0 1px 3px rgba(15,23,42,0.08); margin:12px; padding:16px 20px;";
//    private static final String INPUT =
//        "width:100%; padding:6px 8px; border:1px solid #cbd5e1; border-radius:8px; "
//      + "background:#ffffff; box-sizing:border-box;";
//    private static final String TAKA = "\u09F3 ";
//    private static final String PILL =
//        "display:inline-block; background:#dbeafe; color:#1d4ed8; font-weight:700; "
//      + "padding:2px 12px; border-radius:999px;";
//    private static final String SUBBOX =
//        "background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; padding:10px 12px;";
//
//    // ---- order header
//    private Listbox orgList = new Listbox();
//    private Listbox whList = new Listbox();
//    private Listbox typeList = new Listbox();
//    private Listbox salesTypeList = new Listbox();
//    private Listbox territoryList = new Listbox();
//    private Listbox customerList = new Listbox();
//    private Datebox orderDate = new Datebox();
//    private Datebox deliveryDate = new Datebox();
//
//    // ---- customer card
//    private Label custNameLbl = new Label("");
//    private Label binLbl = new Label("");
//    private Label vatRateLbl = new Label("");
//    private Label creditLimitLbl = new Label("");
//    private Label paymentTermLbl = new Label("");
//    private Label creditDaysLbl = new Label("");
//    private Label contactLbl = new Label("");
//    private Label segmentsLbl = new Label("");
//    private Label addressTitle = new Label("Delivery Address");
//    private Label addressLbl = new Label("");
//    private Checkbox siteDelivery = new Checkbox("Site Delivery");
//    private Textbox siteAddress = new Textbox();
//    private Div siteAddressBox = new Div();
//
//    // ---- add product panel
//    private Listbox segmentList = new Listbox();
//    private Listbox categoryList = new Listbox();
//    private Listbox productList = new Listbox();
//    private Label segWarnLbl = new Label("");
//    private Label pCodeLbl = new Label("");
//    private Label pNameLbl = new Label("");
//    private Label pUomLbl = new Label("");
//    private Label pPriceLbl = new Label("");
//    private Label pTaxLbl = new Label("");
//    private Label stOnHandLbl = new Label("");
//    private Label stReservedLbl = new Label("");
//    private Label stAvailLbl = new Label("");
//    private Label stReqLbl = new Label("");
//    private Label stShortLbl = new Label("");
//    private Decimalbox qtyBox = new Decimalbox();
//    private Label qtyUomLbl = new Label("");
//    private Button addBtn = new Button("+ Add to Order");
//    private Button clearBtn = new Button("Clear");
//
//    // current product state (panel)
//    private String curDescription = "";
//    private String curCode = "";
//    private String curName = "";
//    private String curUom = "";
//    private BigDecimal curPrice = null;
//    private BigDecimal curTaxRate = BigDecimal.ZERO;
//    private BigDecimal curAvailable = BigDecimal.ZERO;
//
//    // ---- order items
//    private Listbox items = new Listbox();
//    private List<Line> lines = new ArrayList<Line>();
//
//    // ---- totals
//    private Label subtotalLbl = new Label(TAKA + "0.00");
//    private Label discountLbl = new Label(TAKA + "0.00");
//    private Label vatLbl = new Label(TAKA + "0.00");
//    private Label grandLbl = new Label(TAKA + "0.00");
//
//    // ---- action buttons
//    private Button saveBtn = new Button("Save Draft");
//    private Button completeBtn = new Button("Complete Order");
//    private Button printBtn = new Button("Print Order");
//    private Button cancelBtn = new Button("Cancel");
//
//    /** One order line (kept in memory until Save Draft is built). */
//    private static class Line {
//        int segId, catId, prodId;
//        String segName, catName, code, name, description, uom;
//        BigDecimal qty, price, taxRate;
//
//        BigDecimal gross() {
//            return qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
//        }
//
//        // VAT on gross amount (paid qty x price, before discount)
//        BigDecimal vat() {
//            return gross().multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//        }
//
//        BigDecimal total() {
//            return gross().add(vat());
//        }
//    }
//
//    @Override
//    protected void initForm() {
//        setStyle("background:#f1f5f9;");
//        ZKUpdateUtil.setWidth(this, "99%");
//        ZKUpdateUtil.setHeight(this, "100%");
//
//        styleInputs();
//
//        Borderlayout main = new Borderlayout();
//        ZKUpdateUtil.setWidth(main, "100%");
//        ZKUpdateUtil.setHeight(main, "100%");
//        appendChild(main);
//
//        North north = new North();
//        ZKUpdateUtil.setVflex(north, "min");
//        north.setStyle("background:transparent; border:none;");
//        main.appendChild(north);
//        north.appendChild(buildTitleBar());
//
//        Center center = new Center();
//        center.setAutoscroll(true);
//        center.setStyle("background:transparent; border:none;");
//        main.appendChild(center);
//
//        Div page = new Div();
//        center.appendChild(page);
//
//        // top row: order info (left) + customer info (right)
//        Div top = new Div();
//        top.setStyle("display:grid; grid-template-columns:repeat(auto-fit, minmax(420px, 1fr)); "
//                   + "align-items:stretch;");
//        top.appendChild(buildOrderInfoCard());
//        top.appendChild(buildCustomerCard());
//        page.appendChild(top);
//
//        page.appendChild(buildAddProductCard());
//        page.appendChild(buildItemsCard());
//
//        salesTypeList.appendItem("Local", "L");
//        salesTypeList.appendItem("Foreign", "F");
//        salesTypeList.setSelectedIndex(0);
//        siteAddressBox.setVisible(false);
//
//        loadOrgs();
//        loadTypes();
//        loadTerritories();
//        refreshLines();
//    }
//
//    // ================================================================== UI
//
//    private void styleInputs() {
//        for (Listbox lb : new Listbox[] { orgList, whList, typeList, salesTypeList,
//                territoryList, customerList, segmentList, categoryList, productList }) {
//            lb.setMold("select");
//            lb.setStyle(INPUT);
//            lb.addEventListener(Events.ON_SELECT, this);
//        }
//        orderDate.setFormat("dd-MMM-yyyy");
//        deliveryDate.setFormat("dd-MMM-yyyy");
//        orderDate.setStyle("width:100%;");
//        deliveryDate.setStyle("width:100%;");
//        orderDate.setValue(new java.util.Date());
//
//        siteDelivery.addEventListener(Events.ON_CHECK, this);
//        siteAddress.setRows(3);
//        siteAddress.setStyle(INPUT);
//        siteAddress.setPlaceholder("Site delivery address");
//
//        qtyBox.setFormat("#,##0.##");
//        qtyBox.setStyle("width:140px; padding:6px 8px; border:1px solid #cbd5e1; border-radius:8px;");
//        qtyBox.addEventListener(Events.ON_CHANGE, this);
//
//        btn(saveBtn, "#2563eb", "#ffffff", null);
//        btn(completeBtn, "#16a34a", "#ffffff", null);
//        btn(printBtn, "#ffffff", "#0f172a", "#cbd5e1");
//        btn(cancelBtn, "#fef2f2", "#dc2626", "#fecaca");
//        btn(addBtn, "#2563eb", "#ffffff", null);
//        btn(clearBtn, "#ffffff", "#334155", "#cbd5e1");
//    }
//
//    private void btn(Button b, String bg, String fg, String border) {
//        b.setStyle("background:" + bg + "; color:" + fg + "; font-weight:600; "
//                 + "padding:8px 18px; border-radius:8px; margin-left:8px; "
//                 + "border:" + (border == null ? "none" : "1px solid " + border) + ";");
//        b.addEventListener(Events.ON_CLICK, this);
//    }
//
//    private Div buildTitleBar() {
//        Div bar = new Div();
//        bar.setStyle("display:flex; flex-wrap:wrap; justify-content:space-between; "
//                   + "align-items:center; gap:8px; margin:12px 12px 0 12px; padding:14px 20px; "
//                   + "background:#ffffff; border:1px solid #e2e8f0; border-radius:12px;");
//
//        Div left = new Div();
//        Label title = new Label("Sales Order Primitek");
//        title.setStyle("display:block; font-size:22px; font-weight:700; color:#0f172a;");
//        Label sub = new Label("Create and manage sales orders");
//        sub.setStyle("display:block; color:#64748b; font-size:13px; margin-top:2px;");
//        left.appendChild(title);
//        left.appendChild(sub);
//
//        Div right = new Div();
//        right.setStyle("display:flex; align-items:center; flex-wrap:wrap;");
//        Label badge = new Label("Draft");
//        badge.setStyle("background:#dbeafe; color:#1d4ed8; font-weight:600; "
//                     + "padding:5px 18px; border-radius:999px; margin-right:8px;");
//        right.appendChild(badge);
//        right.appendChild(saveBtn);
//        right.appendChild(completeBtn);
//        right.appendChild(printBtn);
//        right.appendChild(cancelBtn);
//
//        bar.appendChild(left);
//        bar.appendChild(right);
//        return bar;
//    }
//
//    private Div buildOrderInfoCard() {
//        Div body = card("Order Information");
//        body.setStyle("display:grid; grid-template-columns:130px 1fr 130px 1fr; "
//                    + "gap:10px 16px; align-items:center;");
//        addRow(body, "Organization *", orgList, "Depot / Warehouse *", whList);
//        addRow(body, "Order Date *", orderDate, "Expected Delivery", deliveryDate);
//        addRow(body, "Type of Order", typeList, "Sales Type", salesTypeList);
//        addRow(body, "Territory", territoryList, "Customer *", customerList);
//        return (Div) body.getParent();
//    }
//
//    private Div buildCustomerCard() {
//        Div body = card("Customer Information");
//        body.setStyle("");
//
//        Div kv = new Div();
//        kv.setStyle("display:grid; grid-template-columns:110px 1fr; gap:4px 10px;");
//        custNameLbl.setStyle("font-weight:700; color:#0f172a;");
//        kvRow(kv, "Customer:", custNameLbl);
//        kvRow(kv, "BIN / Tax ID:", binLbl);
//        kvRow(kv, "VAT Rate:", vatRateLbl);
//        kvRow(kv, "Credit Limit:", creditLimitLbl);
//        kvRow(kv, "Payment Term:", paymentTermLbl);
//        kvRow(kv, "Credit Days:", creditDaysLbl);
//        kvRow(kv, "Contact:", contactLbl);
//        kvRow(kv, "Segments:", segmentsLbl);
//        body.appendChild(kv);
//
//        // Site Delivery: tick dile Delivery Address er jayga e site address textbox
//        Div addr = new Div();
//        addr.setStyle(SUBBOX + " margin-top:10px;");
//        siteDelivery.setStyle("font-weight:600; color:#0f172a; display:block; margin-bottom:6px;");
//        addr.appendChild(siteDelivery);
//        addressTitle.setStyle("display:block; font-size:12px; font-weight:600; color:#64748b;");
//        addr.appendChild(addressTitle);
//        addressLbl.setStyle("display:block; color:#0f172a; margin-top:2px;");
//        addr.appendChild(addressLbl);
//        siteAddressBox.appendChild(siteAddress);
//        addr.appendChild(siteAddressBox);
//        body.appendChild(addr);
//        return (Div) body.getParent();
//    }
//
//    private Div buildAddProductCard() {
//        Div body = card("Add Product");
//        body.setStyle("");
//
//        Div row1 = new Div();
//        row1.setStyle("display:grid; grid-template-columns:repeat(auto-fit, minmax(210px, 1fr)); gap:12px;");
//        row1.appendChild(pickCell("Segment *", segmentList));
//        row1.appendChild(pickCell("Category *", categoryList));
//        row1.appendChild(pickCell("Product *", productList));
//
//        Div details = new Div();
//        details.setStyle(SUBBOX + " background:#eff6ff;");
//        Label dt = new Label("Product Details");
//        dt.setStyle("display:block; font-weight:700; color:#1d4ed8; margin-bottom:4px;");
//        details.appendChild(dt);
//        Div dkv = new Div();
//        dkv.setStyle("display:grid; grid-template-columns:70px 1fr; gap:2px 8px;");
//        kvRow(dkv, "Code:", pCodeLbl);
//        kvRow(dkv, "Name:", pNameLbl);
//        kvRow(dkv, "UOM:", pUomLbl);
//        kvRow(dkv, "Price:", pPriceLbl);
//        kvRow(dkv, "Tax:", pTaxLbl);
//        details.appendChild(dkv);
//        row1.appendChild(details);
//
//        Div stock = new Div();
//        stock.setStyle(SUBBOX + " background:#f0fdf4;");
//        Label st = new Label("Stock (selected warehouse)");
//        st.setStyle("display:block; font-weight:700; color:#15803d; margin-bottom:4px;");
//        stock.appendChild(st);
//        Div skv = new Div();
//        skv.setStyle("display:grid; grid-template-columns:90px 1fr; gap:2px 8px;");
//        kvRow(skv, "On Hand:", stOnHandLbl);
//        kvRow(skv, "Reserved:", stReservedLbl);
//        kvRow(skv, "Available:", stAvailLbl);
//        kvRow(skv, "Requested:", stReqLbl);
//        kvRow(skv, "Shortage:", stShortLbl);
//        stock.appendChild(skv);
//        row1.appendChild(stock);
//        body.appendChild(row1);
//
//        segWarnLbl.setStyle("display:block; color:#b45309; margin-top:6px;");
//        body.appendChild(segWarnLbl);
//
//        // NOTE: category-wise Product Attributes row will be added in Stage 2.
//
//        Div row2 = new Div();
//        row2.setStyle("display:flex; flex-wrap:wrap; align-items:center; gap:12px; margin-top:12px;");
//        Label ql = new Label("Order Quantity *");
//        ql.setStyle("font-weight:600; color:#334155;");
//        qtyUomLbl.setStyle("color:#64748b;");
//        row2.appendChild(ql);
//        row2.appendChild(qtyBox);
//        row2.appendChild(qtyUomLbl);
//        body.appendChild(row2);
//
//        Div row3 = new Div();
//        row3.setStyle("text-align:right; margin-top:10px;");
//        row3.appendChild(addBtn);
//        row3.appendChild(clearBtn);
//        body.appendChild(row3);
//        return (Div) body.getParent();
//    }
//
//    private Div pickCell(String title, Listbox lb) {
//        Div d = new Div();
//        d.setStyle(SUBBOX);
//        Label l = new Label(title);
//        l.setStyle("display:block; font-weight:600; color:#334155; margin-bottom:4px;");
//        d.appendChild(l);
//        d.appendChild(lb);
//        return d;
//    }
//
//    private void kvRow(Div g, String k, Label v) {
//        Label kl = new Label(k);
//        kl.setStyle("color:#64748b; font-size:12px;");
//        if (v.getStyle() == null || v.getStyle().isEmpty())
//            v.setStyle("color:#0f172a;");
//        g.appendChild(kl);
//        g.appendChild(v);
//    }
//
//    private Div buildItemsCard() {
//        Div body = card("Order Items");
//        body.setStyle("");
//
//        items.setStyle("width:100%;");
//        items.setEmptyMessage("No product added yet.");
//        Listhead head = new Listhead();
//        String[][] cols = { { "#", "40px" }, { "Segment", "110px" }, { "Category", "110px" },
//                { "Product", "170px" }, { "Description", null }, { "UOM", "60px" },
//                { "Qty", "80px" }, { "Price", "100px" }, { "Gross", "110px" },
//                { "VAT", "100px" }, { "Total", "120px" }, { "Action", "130px" } };
//        for (String[] c : cols) {
//            Listheader h = new Listheader(c[0]);
//            if (c[1] != null)
//                h.setWidth(c[1]);
//            else
//                h.setHflex("1");
//            head.appendChild(h);
//        }
//        items.appendChild(head);
//        body.appendChild(items);
//
//        Div totals = new Div();
//        totals.setStyle("display:grid; grid-template-columns:1fr 140px; gap:4px 12px; "
//                      + "width:300px; margin:14px 0 0 auto; text-align:right;");
//        totals.appendChild(new Label("Subtotal (Gross)"));
//        totals.appendChild(subtotalLbl);
//        totals.appendChild(new Label("Discount"));
//        totals.appendChild(discountLbl);
//        totals.appendChild(new Label("VAT / Tax"));
//        totals.appendChild(vatLbl);
//        Label gt = new Label("Grand Total");
//        gt.setStyle("font-weight:700; font-size:15px; color:#15803d;");
//        grandLbl.setStyle("font-weight:700; font-size:15px; color:#15803d;");
//        totals.appendChild(gt);
//        totals.appendChild(grandLbl);
//        body.appendChild(totals);
//        return (Div) body.getParent();
//    }
//
//    /** Creates a card with a title; returns the BODY div (card = body.getParent()). */
//    private Div card(String title) {
//        Div card = new Div();
//        card.setStyle(CARD);
//        Label t = new Label(title);
//        t.setStyle("display:block; font-size:16px; font-weight:700; color:#1d4ed8; "
//                 + "margin-bottom:12px;");
//        card.appendChild(t);
//        Div body = new Div();
//        card.appendChild(body);
//        return body;
//    }
//
//    private void addRow(Div g, String l1, Component c1, String l2, Component c2) {
//        g.appendChild(fieldLabel(l1));
//        g.appendChild(c1);
//        g.appendChild(fieldLabel(l2));
//        g.appendChild(c2);
//    }
//
//    private Label fieldLabel(String text) {
//        Label l = new Label(text);
//        l.setStyle("font-weight:600; color:#334155;");
//        return l;
//    }
//
//    // ============================================================== loaders
//
//    private int clientId() {
//        return Env.getAD_Client_ID(Env.getCtx());
//    }
//
//    private void loadOrgs() {
//        fill(orgList,
//            "SELECT AD_Org_ID, Name FROM AD_Org WHERE IsActive='Y' AND IsSummary='N' "
//          + "AND AD_Client_ID=? AND AD_Org_ID>0 ORDER BY Name",
//            true, clientId());
//        if (orgList.getItemCount() == 2)
//            orgList.setSelectedIndex(1);
//        loadWarehouses();
//    }
//
//    private void loadWarehouses() {
//        int org = selectedId(orgList);
//        fill(whList,
//            "SELECT M_Warehouse_ID, Name FROM M_Warehouse WHERE IsActive='Y' "
//          + "AND AD_Client_ID=? AND AD_Org_ID=? ORDER BY Name",
//            true, clientId(), org);
//        if (whList.getItemCount() == 2)
//            whList.setSelectedIndex(1);
//        refreshStock();
//    }
//
//    private void loadTypes() {
//        fill(typeList,
//            "SELECT C_DocType_ID, Name FROM C_DocType WHERE IsActive='Y' "
//          + "AND AD_Client_ID=? AND DocBaseType='SOO' ORDER BY Name",
//            true, clientId());
//        if (typeList.getItemCount() == 2)
//            typeList.setSelectedIndex(1);
//    }
//
//    /** Territory: Sales Type onujayi. */
//    private void loadTerritories() {
//        String st = selectedValue(salesTypeList);
//        fill(territoryList,
//            "SELECT t.XX_Territory_ID, t.Name FROM XX_Territory t "
//          + "WHERE t.IsActive='Y' AND t.AD_Client_ID=? AND t.SalesType=? ORDER BY t.Name",
//            true, clientId(), st);
//        loadCustomers();
//    }
//
//    /** Customer: Sales Type + Territory (blank = National). */
//    private void loadCustomers() {
//        int terr = selectedId(territoryList);
//        String st = selectedValue(salesTypeList);
//
//        StringBuilder sql = new StringBuilder(
//            "SELECT bp.C_BPartner_ID, bp.Name FROM C_BPartner bp "
//          + "WHERE bp.IsActive='Y' AND bp.IsCustomer='Y' AND bp.AD_Client_ID=? ");
//        List<Object> params = new ArrayList<Object>();
//        params.add(clientId());
//
//        if (terr > 0) {
//            sql.append("AND bp.XX_Territory_ID=? ");
//            params.add(terr);
//        } else {
//            sql.append("AND EXISTS (SELECT 1 FROM XX_Territory t "
//                     + "  WHERE t.XX_Territory_ID=bp.XX_Territory_ID AND t.SalesType=?) ");
//            params.add(st);
//        }
//        sql.append("ORDER BY bp.Name");
//
//        fill(customerList, sql.toString(), true, params.toArray());
//        lines.clear();
//        refreshLines();
//        clearCustomerInfo();
//    }
//
//    private void loadCustomerInfo(int bpId) {
//        clearCustomerInfo();
//        if (bpId <= 0)
//            return;
//
//        custNameLbl.setValue(customerList.getSelectedItem().getLabel());
//
//        addressLbl.setValue(queryString(
//            "SELECT COALESCE(l.Address1,'') || CASE WHEN l.City IS NULL THEN '' ELSE ', ' || l.City END "
//          + "FROM C_BPartner_Location bl JOIN C_Location l ON l.C_Location_ID=bl.C_Location_ID "
//          + "WHERE bl.C_BPartner_ID=? AND bl.IsActive='Y' ORDER BY bl.C_BPartner_Location_ID LIMIT 1",
//            bpId));
//
//        String[] u = queryRow(
//            "SELECT COALESCE(Name,''), COALESCE(Phone,'') FROM AD_User "
//          + "WHERE C_BPartner_ID=? AND IsActive='Y' ORDER BY AD_User_ID LIMIT 1", 2, bpId);
//        contactLbl.setValue(u[1].isEmpty() ? u[0] : u[0] + " (" + u[1] + ")");
//
//        String[] c = queryRow(
//            "SELECT COALESCE(bp.TaxID,''), "
//          + "to_char(COALESCE(bp.SO_CreditLimit,0),'FM999,999,999,990.00'), "
//          + "COALESCE(pt.Name,''), "
//          + "COALESCE(bp.XX_CreditDays, pt.NetDays, 0)::text, "
//          + "to_char(COALESCE(bp.XX_VATRate,0),'FM990.00') "
//          + "FROM C_BPartner bp LEFT JOIN C_PaymentTerm pt "
//          + "ON pt.C_PaymentTerm_ID=bp.C_PaymentTerm_ID WHERE bp.C_BPartner_ID=?", 5, bpId);
//        binLbl.setValue(c[0]);
//        creditLimitLbl.setValue(TAKA + c[1]);
//        paymentTermLbl.setValue(c[2]);
//        creditDaysLbl.setValue(c[3] + " days");
//        vatRateLbl.setValue(c[4] + " %");
//
//        // customer er segment gulo = Segment dropdown er option
//        loadSegments(bpId);
//    }
//
//    private void clearCustomerInfo() {
//        custNameLbl.setValue("");
//        binLbl.setValue("");
//        vatRateLbl.setValue("");
//        creditLimitLbl.setValue("");
//        paymentTermLbl.setValue("");
//        creditDaysLbl.setValue("");
//        contactLbl.setValue("");
//        segmentsLbl.setValue("");
//        addressLbl.setValue("");
//        segWarnLbl.setValue("");
//        clearList(segmentList);
//        clearList(categoryList);
//        clearList(productList);
//        clearProductInfo();
//        qtyBox.setValue((BigDecimal) null);
//    }
//
//    // ---- Segment -> Category -> Product
//
//    private void loadSegments(int bpId) {
//        fill(segmentList,
//            "SELECT s.XX_Segment_ID, s.Name FROM XX_Segment s "
//          + "JOIN XX_BPartnerSegment bs ON bs.XX_Segment_ID=s.XX_Segment_ID "
//          + "WHERE bs.C_BPartner_ID=? AND bs.IsActive='Y' AND s.IsActive='Y' "
//          + "AND s.AD_Client_ID=? ORDER BY s.Name",
//            true, bpId, clientId());
//        StringBuilder names = new StringBuilder();
//        for (int i = 1; i < segmentList.getItemCount(); i++) {
//            if (names.length() > 0)
//                names.append(", ");
//            names.append(segmentList.getItemAtIndex(i).getLabel());
//        }
//        segmentsLbl.setValue(names.toString());
//        segWarnLbl.setValue(segmentList.getItemCount() <= 1
//                ? "This customer has no segment assigned. Assign a segment to add products." : "");
//        loadCategories();
//    }
//
//    private void loadCategories() {
//        int seg = selectedId(segmentList);
//        fill(categoryList,
//            "SELECT pc.M_Product_Category_ID, pc.Name FROM M_Product_Category pc "
//          + "JOIN XX_SegmentCategory sc ON sc.M_Product_Category_ID=pc.M_Product_Category_ID "
//          + "WHERE sc.XX_Segment_ID=? AND sc.IsActive='Y' AND pc.IsActive='Y' ORDER BY pc.Name",
//            true, seg);
//        loadProducts();
//    }
//
//    private void loadProducts() {
//        int cat = selectedId(categoryList);
//        fill(productList,
//            "SELECT M_Product_ID, Value || ' - ' || Name FROM M_Product "
//          + "WHERE M_Product_Category_ID=? AND IsActive='Y' AND IsSold='Y' AND IsSummary='N' "
//          + "AND AD_Client_ID=? ORDER BY Name",
//            true, cat, clientId());
//        clearProductInfo();
//    }
//
//    private void clearProductInfo() {
//        curDescription = "";
//        curCode = "";
//        curName = "";
//        curUom = "";
//        curPrice = null;
//        curTaxRate = BigDecimal.ZERO;
//        curAvailable = BigDecimal.ZERO;
//        pCodeLbl.setValue("");
//        pNameLbl.setValue("");
//        pUomLbl.setValue("");
//        pPriceLbl.setValue("");
//        pTaxLbl.setValue("");
//        qtyUomLbl.setValue("");
//        stOnHandLbl.setValue("");
//        stReservedLbl.setValue("");
//        stAvailLbl.setValue("");
//        stReqLbl.setValue("");
//        stShortLbl.setValue("");
//        stShortLbl.setStyle("color:#0f172a;");
//    }
//
//    private void loadProductInfo(int prodId) {
//        clearProductInfo();
//        if (prodId <= 0)
//            return;
//
//        // (server side) product must really belong to the selected category
//        String[] p = queryRow(
//            "SELECT p.Value, p.Name, COALESCE(p.Description,''), COALESCE(u.UOMSymbol,u.Name), "
//          + "COALESCE(p.C_TaxCategory_ID,0)::text FROM M_Product p "
//          + "JOIN C_UOM u ON u.C_UOM_ID=p.C_UOM_ID "
//          + "WHERE p.M_Product_ID=? AND p.M_Product_Category_ID=?",
//            5, prodId, selectedId(categoryList));
//        if (p[0].isEmpty())
//            return;
//        curCode = p[0];
//        curName = p[1];
//        curDescription = p[2].isEmpty() ? p[1] : p[2];
//        curUom = p[3];
//        pCodeLbl.setValue(curCode);
//        pNameLbl.setValue(curName);
//        pUomLbl.setValue(curUom);
//        qtyUomLbl.setValue(curUom);
//
//        curPrice = findPrice(prodId);
//        pPriceLbl.setValue(curPrice == null ? "No price in Price List" : TAKA + fmt(curPrice));
//
//        curTaxRate = findTaxRate(parseInt(p[4]));
//        pTaxLbl.setValue(fmt(curTaxRate) + " %");
//
//        refreshStock();
//    }
//
//    /** Customer er Price List age, na thakle shob Sales Price List theke (order date e valid version). */
//    private BigDecimal findPrice(int prodId) {
//        java.util.Date d = orderDate.getValue();
//        Timestamp ts = new Timestamp(d == null ? System.currentTimeMillis() : d.getTime());
//        String s = queryString(
//            "SELECT pp.PriceStd::text FROM M_ProductPrice pp "
//          + "JOIN M_PriceList_Version plv ON plv.M_PriceList_Version_ID=pp.M_PriceList_Version_ID "
//          + "JOIN M_PriceList pl ON pl.M_PriceList_ID=plv.M_PriceList_ID "
//          + "WHERE pp.M_Product_ID=? AND pp.IsActive='Y' AND pl.IsSOPriceList='Y' "
//          + "AND pl.IsActive='Y' AND pl.AD_Client_ID=? AND plv.IsActive='Y' AND plv.ValidFrom<=? "
//          + "ORDER BY CASE WHEN pl.M_PriceList_ID=(SELECT M_PriceList_ID FROM C_BPartner "
//          + "WHERE C_BPartner_ID=?) THEN 0 ELSE 1 END, plv.ValidFrom DESC LIMIT 1",
//            prodId, clientId(), ts, selectedId(customerList));
//        return s.isEmpty() ? null : new BigDecimal(s);
//    }
//
//    /** Product Tax Category theke C_Tax rate (simple lookup, full tax determination pore). */
//    private BigDecimal findTaxRate(int taxCategoryId) {
//        if (taxCategoryId <= 0)
//            return BigDecimal.ZERO;
//        String s = queryString(
//            "SELECT COALESCE(Rate,0)::text FROM C_Tax WHERE C_TaxCategory_ID=? AND IsActive='Y' "
//          + "AND ValidFrom<=now() AND SOPOType IN ('B','S') "
//          + "ORDER BY IsDefault DESC, ValidFrom DESC LIMIT 1", taxCategoryId);
//        return s.isEmpty() ? BigDecimal.ZERO : new BigDecimal(s);
//    }
//
//    /** Stock of selected product in selected warehouse (iDempiere storage tables, read only). */
//    private void refreshStock() {
//        int prod = selectedId(productList);
//        int wh = selectedId(whList);
//        if (prod <= 0 || wh <= 0) {
//            stOnHandLbl.setValue("");
//            stReservedLbl.setValue("");
//            stAvailLbl.setValue("");
//            stReqLbl.setValue("");
//            stShortLbl.setValue("");
//            curAvailable = BigDecimal.ZERO;
//            return;
//        }
//        BigDecimal onHand = num(queryString(
//            "SELECT COALESCE(SUM(oh.QtyOnHand),0)::text FROM M_StorageOnHand oh "
//          + "JOIN M_Locator l ON l.M_Locator_ID=oh.M_Locator_ID "
//          + "WHERE oh.M_Product_ID=? AND l.M_Warehouse_ID=?", prod, wh));
//        BigDecimal reserved = num(queryString(
//            "SELECT COALESCE(SUM(Qty),0)::text FROM M_StorageReservation "
//          + "WHERE M_Product_ID=? AND M_Warehouse_ID=? AND IsSOTrx='Y'", prod, wh));
//        curAvailable = onHand.subtract(reserved);
//
//        stOnHandLbl.setValue(fmt(onHand));
//        stReservedLbl.setValue(fmt(reserved));
//        stAvailLbl.setValue(fmt(curAvailable));
//        updateRequested();
//    }
//
//    private void updateRequested() {
//        BigDecimal q = qtyBox.getValue();
//        if (q == null || selectedId(productList) <= 0) {
//            stReqLbl.setValue("");
//            stShortLbl.setValue("");
//            return;
//        }
//        stReqLbl.setValue(fmt(q));
//        BigDecimal shortage = q.subtract(curAvailable);
//        if (shortage.signum() > 0) {
//            stShortLbl.setValue(fmt(shortage) + "  (warning)");
//            stShortLbl.setStyle("color:#dc2626; font-weight:700;");
//        } else {
//            stShortLbl.setValue("0");
//            stShortLbl.setStyle("color:#0f172a;");
//        }
//    }
//
//    // ============================================================ order lines
//
//    private void addLine() {
//        int bp = selectedId(customerList);
//        int seg = selectedId(segmentList);
//        int cat = selectedId(categoryList);
//        int prod = selectedId(productList);
//        BigDecimal q = qtyBox.getValue();
//
//        if (bp <= 0)
//            { warn("Select a customer first."); return; }
//        if (selectedId(whList) <= 0)
//            { warn("Select a Depot / Warehouse."); return; }
//        if (seg <= 0 || cat <= 0 || prod <= 0)
//            { warn("Select Segment, Category and Product."); return; }
//        if (q == null || q.signum() <= 0)
//            { warn("Enter a quantity greater than zero."); return; }
//        if (curPrice == null)
//            { warn("This product has no price in the Price List."); return; }
//
//        // server side check: customer -> segment -> category -> product combination
//        String ok = queryString(
//            "SELECT 1 FROM M_Product p "
//          + "JOIN XX_SegmentCategory sc ON sc.M_Product_Category_ID=p.M_Product_Category_ID "
//          + "AND sc.IsActive='Y' "
//          + "JOIN XX_BPartnerSegment bs ON bs.XX_Segment_ID=sc.XX_Segment_ID AND bs.IsActive='Y' "
//          + "WHERE p.M_Product_ID=? AND sc.XX_Segment_ID=? AND sc.M_Product_Category_ID=? "
//          + "AND bs.C_BPartner_ID=? AND p.AD_Client_ID=? LIMIT 1",
//            prod, seg, cat, bp, clientId());
//        if (ok.isEmpty())
//            { warn("Invalid Segment / Category / Product combination for this customer."); return; }
//
//        for (Line l : lines) {
//            if (l.prodId == prod)
//                { warn("This product is already in the order. Edit or remove that line."); return; }
//        }
//
//        Line l = new Line();
//        l.segId = seg;
//        l.catId = cat;
//        l.prodId = prod;
//        l.segName = segmentList.getSelectedItem().getLabel();
//        l.catName = categoryList.getSelectedItem().getLabel();
//        l.code = curCode;
//        l.name = curName;
//        l.description = curDescription;
//        l.uom = curUom;
//        l.qty = q;
//        l.price = curPrice;
//        l.taxRate = curTaxRate;
//        lines.add(l);
//
//        refreshLines();
//        clearPanel();
//    }
//
//    private void editLine(int idx) {
//        if (idx < 0 || idx >= lines.size())
//            return;
//        Line l = lines.remove(idx);
//        refreshLines();
//
//        selectById(segmentList, l.segId);
//        loadCategories();
//        selectById(categoryList, l.catId);
//        loadProducts();
//        selectById(productList, l.prodId);
//        loadProductInfo(l.prodId);
//        qtyBox.setValue(l.qty);
//        updateRequested();
//    }
//
//    private void removeLine(int idx) {
//        if (idx < 0 || idx >= lines.size())
//            return;
//        lines.remove(idx);
//        refreshLines();
//    }
//
//    private void clearPanel() {
//        if (segmentList.getItemCount() > 0)
//            segmentList.setSelectedIndex(0);
//        loadCategories();
//        qtyBox.setValue((BigDecimal) null);
//        stReqLbl.setValue("");
//        stShortLbl.setValue("");
//    }
//
//    private void refreshLines() {
//        items.getItems().clear();
//        BigDecimal gross = BigDecimal.ZERO;
//        BigDecimal vat = BigDecimal.ZERO;
//        int n = 0;
//        for (Line l : lines) {
//            Listitem li = new Listitem();
//            li.appendChild(new Listcell(String.valueOf(++n)));
//            li.appendChild(new Listcell(l.segName));
//            li.appendChild(new Listcell(l.catName));
//            li.appendChild(new Listcell(l.name));
//            li.appendChild(new Listcell(l.description));
//            li.appendChild(new Listcell(l.uom));
//            li.appendChild(new Listcell(fmt(l.qty)));
//            li.appendChild(new Listcell(TAKA + fmt(l.price)));
//            li.appendChild(new Listcell(TAKA + fmt(l.gross())));
//            li.appendChild(new Listcell(TAKA + fmt(l.vat()) + " (" + fmt(l.taxRate) + "%)"));
//            li.appendChild(new Listcell(TAKA + fmt(l.total())));
//
//            Listcell act = new Listcell();
//            Button e = new Button("Edit");
//            e.setAttribute("act", "edit");
//            e.setAttribute("idx", Integer.valueOf(n - 1));
//            e.addEventListener(Events.ON_CLICK, this);
//            Button r = new Button("Remove");
//            r.setAttribute("act", "remove");
//            r.setAttribute("idx", Integer.valueOf(n - 1));
//            r.addEventListener(Events.ON_CLICK, this);
//            act.appendChild(e);
//            act.appendChild(r);
//            li.appendChild(act);
//            items.appendChild(li);
//
//            gross = gross.add(l.gross());
//            vat = vat.add(l.vat());
//        }
//        subtotalLbl.setValue(TAKA + fmt(gross));
//        discountLbl.setValue(TAKA + "0.00");
//        vatLbl.setValue(TAKA + fmt(vat));
//        grandLbl.setValue(TAKA + fmt(gross.add(vat)));
//    }
//
//    // ================================================================ events
//
//    @Override
//    public void onEvent(Event e) {
//        Object t = e.getTarget();
//
//        if (t instanceof Component) {
//            Object act = ((Component) t).getAttribute("act");
//            if (act != null) {
//                int idx = ((Integer) ((Component) t).getAttribute("idx")).intValue();
//                if ("edit".equals(act))
//                    editLine(idx);
//                else if ("remove".equals(act))
//                    removeLine(idx);
//                return;
//            }
//        }
//
//        if (t == orgList) {
//            loadWarehouses();
//        } else if (t == whList) {
//            refreshStock();
//        } else if (t == salesTypeList) {
//            loadTerritories();
//        } else if (t == territoryList) {
//            loadCustomers();
//        } else if (t == customerList) {
//            if (!lines.isEmpty()) {
//                lines.clear();
//                refreshLines();
//                warn("Customer changed. Order lines were cleared.");
//            }
//            loadCustomerInfo(selectedId(customerList));
//        } else if (t == segmentList) {
//            loadCategories();
//        } else if (t == categoryList) {
//            loadProducts();
//        } else if (t == productList) {
//            loadProductInfo(selectedId(productList));
//        } else if (t == qtyBox) {
//            updateRequested();
//        } else if (t == addBtn) {
//            addLine();
//        } else if (t == clearBtn) {
//            clearPanel();
//        } else if (t == siteDelivery) {
//            applySiteDelivery(siteDelivery.isChecked());
//        } else if (t == cancelBtn) {
//            resetForm();
//        } else if (t == saveBtn || t == completeBtn || t == printBtn) {
//            Messagebox.show("Later",
//                    "Coming next", Messagebox.OK, Messagebox.INFORMATION);
//        }
//    }
//
//    /** Site Delivery on: customer address er jayga e site address textbox. */
//    private void applySiteDelivery(boolean on) {
//        addressTitle.setValue(on ? "Site Delivery Address" : "Delivery Address");
//        addressLbl.setVisible(!on);
//        siteAddressBox.setVisible(on);
//        if (!on)
//            siteAddress.setValue("");
//    }
//
//    private void resetForm() {
//        orderDate.setValue(new java.util.Date());
//        deliveryDate.setValue(null);
//        salesTypeList.setSelectedIndex(0);
//        siteDelivery.setChecked(false);
//        applySiteDelivery(false);
//        loadOrgs();
//        loadTypes();
//        loadTerritories();
//    }
//
//    private void warn(String msg) {
//        Messagebox.show(msg, "Sales Order", Messagebox.OK, Messagebox.EXCLAMATION);
//    }
//
//    // =============================================================== utility
//
//    private String fmt(BigDecimal v) {
//        return new DecimalFormat("#,##0.00").format(v == null ? BigDecimal.ZERO : v);
//    }
//
//    private BigDecimal num(String s) {
//        try {
//            return (s == null || s.isEmpty()) ? BigDecimal.ZERO : new BigDecimal(s);
//        } catch (Exception ex) {
//            return BigDecimal.ZERO;
//        }
//    }
//
//    private int parseInt(String s) {
//        try {
//            return Integer.parseInt(s);
//        } catch (Exception ex) {
//            return 0;
//        }
//    }
//
//    private int selectedId(Listbox lb) {
//        return parseInt(selectedValue(lb));
//    }
//
//    private String selectedValue(Listbox lb) {
//        if (lb.getSelectedItem() == null)
//            return "";
//        Object v = lb.getSelectedItem().getValue();
//        return v == null ? "" : v.toString();
//    }
//
//    private void selectById(Listbox lb, int id) {
//        String v = String.valueOf(id);
//        for (Listitem li : lb.getItems()) {
//            if (v.equals(li.getValue())) {
//                lb.setSelectedItem(li);
//                return;
//            }
//        }
//    }
//
//    private void clearList(Listbox lb) {
//        lb.getItems().clear();
//        lb.appendItem("", "0");
//        lb.setSelectedIndex(0);
//    }
//
//    private void fill(Listbox lb, String sql, boolean blank, Object... params) {
//        lb.getItems().clear();
//        if (blank)
//            lb.appendItem("", "0");
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            ps = DB.prepareStatement(sql, null);
//            DB.setParameters(ps, params);
//            rs = ps.executeQuery();
//            while (rs.next())
//                lb.appendItem(rs.getString(2), String.valueOf(rs.getInt(1)));
//        } catch (Exception ex) {
//            throw new RuntimeException(sql, ex);
//        } finally {
//            DB.close(rs, ps);
//        }
//        if (lb.getItemCount() > 0)
//            lb.setSelectedIndex(0);
//    }
//
//    private String queryString(String sql, Object... params) {
//        return queryRow(sql, 1, params)[0];
//    }
//
//    private String[] queryRow(String sql, int cols, Object... params) {
//        String[] out = new String[cols];
//        for (int i = 0; i < cols; i++)
//            out[i] = "";
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            ps = DB.prepareStatement(sql, null);
//            DB.setParameters(ps, params);
//            rs = ps.executeQuery();
//            if (rs.next()) {
//                for (int i = 0; i < cols; i++) {
//                    String v = rs.getString(i + 1);
//                    out[i] = v == null ? "" : v;
//                }
//            }
//        } catch (Exception ex) {
//            throw new RuntimeException(sql, ex);
//        } finally {
//            DB.close(rs, ps);
//        }
//        return out;
//    }
//}