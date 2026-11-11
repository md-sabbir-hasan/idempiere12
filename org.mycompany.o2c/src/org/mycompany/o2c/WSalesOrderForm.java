package org.mycompany.o2c;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;

public class WSalesOrderForm extends ADForm implements EventListener<Event> {
    private static final long serialVersionUID = 1L;

    private static final String CARD =
        "background:#ffffff; border:1px solid #e2e8f0; border-radius:12px; "
      + "box-shadow:0 1px 3px rgba(15,23,42,0.08); margin:12px; padding:16px 20px;";
    private static final String INPUT =
        "width:100%; padding:6px 8px; border:1px solid #cbd5e1; border-radius:8px; "
      + "background:#ffffff; box-sizing:border-box;";
    private static final String TAKA = "\u09F3 ";

    // header
    private Listbox orgList = new Listbox();
    private Listbox whList = new Listbox();
    private Listbox typeList = new Listbox();
    private Listbox segmentList = new Listbox();
    private Listbox salesTypeList = new Listbox();
    private Listbox territoryList = new Listbox();
    private Listbox customerList = new Listbox();
    private Datebox orderDate = new Datebox();
    private Datebox deliveryDate = new Datebox();

    // customer card
    private Label custNameLbl = new Label("");
    private Label contactLbl = new Label("");
    private Label phoneLbl = new Label("");
    private Label emailLbl = new Label("");
    private Label addressLbl = new Label("");
    private Label creditLimitLbl = new Label("");
    private Label paymentTermLbl = new Label("");
    private Checkbox siteDelivery = new Checkbox("Site Delivery");
    private Textbox siteAddress = new Textbox();
    private Div siteAddressBox = new Div();

    // totals (placeholder, porer step e hisab hobe)
    private Label subtotalLbl = new Label(TAKA + "0.00");
    private Label discountLbl = new Label(TAKA + "0.00");
    private Label vatLbl = new Label(TAKA + "0.00");
    private Label grandLbl = new Label(TAKA + "0.00");

    // buttons
    private Button saveBtn = new Button("Save Draft");
    private Button completeBtn = new Button("Complete Order");
    private Button printBtn = new Button("Print Order");
    private Button cancelBtn = new Button("Cancel");
    private Button addProductBtn = new Button("+ Add Product");

    @Override
    protected void initForm() {
        setStyle("background:#f1f5f9;");
        ZKUpdateUtil.setWidth(this, "99%");
        ZKUpdateUtil.setHeight(this, "100%");

        styleInputs();

        Borderlayout main = new Borderlayout();
        ZKUpdateUtil.setWidth(main, "100%");
        ZKUpdateUtil.setHeight(main, "100%");
        appendChild(main);

        // ---------- North: title bar ----------
        North north = new North();
        ZKUpdateUtil.setVflex(north, "min");
        north.setStyle("background:transparent; border:none;");
        main.appendChild(north);
        north.appendChild(buildTitleBar());

        // ---------- Center: cards (scrollable) ----------
        Center center = new Center();
        center.setAutoscroll(true);
        center.setStyle("background:transparent; border:none;");
        main.appendChild(center);

        Div page = new Div();
        center.appendChild(page);
        page.appendChild(buildOrderInfoCard());
        page.appendChild(buildCustomerCard());
        page.appendChild(buildItemsCard());

        // ---------- South: action bar ----------
        South south = new South();
        ZKUpdateUtil.setVflex(south, "min");
        south.setStyle("background:#ffffff; border-top:1px solid #e2e8f0;");
        main.appendChild(south);
        south.appendChild(buildActionBar());

        // defaults
        salesTypeList.appendItem("Local", "L");
        salesTypeList.appendItem("Foreign", "F");
        salesTypeList.setSelectedIndex(0);
        siteAddressBox.setVisible(false);

        loadOrgs();
        loadTypes();
        loadSegments();
    }

    // ================================================================== UI

    private void styleInputs() {
        for (Listbox lb : new Listbox[] { orgList, whList, typeList, segmentList,
                salesTypeList, territoryList, customerList }) {
            lb.setMold("select");
            lb.setStyle(INPUT);
            lb.addEventListener(Events.ON_SELECT, this);
        }
        orderDate.setFormat("dd-MMM-yyyy");
        deliveryDate.setFormat("dd-MMM-yyyy");
        orderDate.setStyle("width:100%;");
        deliveryDate.setStyle("width:100%;");
        orderDate.setValue(new java.util.Date());

        siteDelivery.addEventListener(Events.ON_CHECK, this);
        siteDelivery.setStyle("font-weight:600; color:#0f172a;");
        siteAddress.setRows(2);
        siteAddress.setStyle(INPUT);
        siteAddress.setPlaceholder("Site delivery address");

        btn(saveBtn, "#2563eb", "#ffffff", null);
        btn(completeBtn, "#16a34a", "#ffffff", null);
        btn(printBtn, "#ffffff", "#0f172a", "#cbd5e1");
        btn(cancelBtn, "#fef2f2", "#dc2626", "#fecaca");
        btn(addProductBtn, "#2563eb", "#ffffff", null);
    }

    private void btn(Button b, String bg, String fg, String border) {
        b.setStyle("background:" + bg + "; color:" + fg + "; font-weight:600; "
                 + "padding:8px 18px; border-radius:8px; margin-left:8px; "
                 + "border:" + (border == null ? "none" : "1px solid " + border) + ";");
        b.addEventListener(Events.ON_CLICK, this);
    }

    private Div buildTitleBar() {
        Div bar = new Div();
        bar.setStyle("display:flex; justify-content:space-between; align-items:center; "
                   + "margin:12px 12px 0 12px; padding:14px 20px; background:#ffffff; "
                   + "border:1px solid #e2e8f0; border-radius:12px;");

        Div left = new Div();
        Label title = new Label("Sales Order Primitek");
        title.setStyle("display:block; font-size:22px; font-weight:700; color:#0f172a;");
        Label sub = new Label("Create and manage sales orders");
        sub.setStyle("display:block; color:#64748b; font-size:13px; margin-top:2px;");
        left.appendChild(title);
        left.appendChild(sub);

        Label badge = new Label("Draft");
        badge.setStyle("background:#dbeafe; color:#1d4ed8; font-weight:600; "
                     + "padding:5px 18px; border-radius:999px;");

        bar.appendChild(left);
        bar.appendChild(badge);
        return bar;
    }

    private Div buildOrderInfoCard() {
        Div body = card("Order Information");
        body.setStyle("display:grid; grid-template-columns:150px 1fr 150px 1fr; "
                    + "gap:10px 16px; align-items:center;");

        addRow(body, "Organization *", orgList, "Depot / Warehouse", whList);
        addRow(body, "Order Date *", orderDate, "Expected Delivery", deliveryDate);
        addRow(body, "Type of Order", typeList, "Segment", segmentList);
        addRow(body, "Sales Type", salesTypeList, "Territory", territoryList);
        addRow(body, "Customer", customerList, "", new Div());
        return (Div) body.getParent();
    }

    private Div buildCustomerCard() {
        Div body = card("Customer Information");
        body.setStyle("display:grid; grid-template-columns:repeat(4, 1fr); gap:16px;");

        // col 1: who
        Div c1 = new Div();
        custNameLbl.setStyle("display:block; font-size:16px; font-weight:700; color:#0f172a;");
        contactLbl.setStyle("display:block; color:#334155; margin-top:6px;");
        phoneLbl.setStyle("display:block; color:#334155;");
        emailLbl.setStyle("display:block; color:#334155;");
        c1.appendChild(custNameLbl);
        c1.appendChild(contactLbl);
        c1.appendChild(phoneLbl);
        c1.appendChild(emailLbl);

        // col 2: address
        Div c2 = new Div();
        c2.setStyle("border-left:1px solid #e2e8f0; padding-left:16px;");
        c2.appendChild(small("Delivery Address"));
        addressLbl.setStyle("display:block; color:#0f172a; margin-top:4px;");
        c2.appendChild(addressLbl);

        // col 3: credit + payment
        Div c3 = new Div();
        c3.setStyle("border-left:1px solid #e2e8f0; padding-left:16px;");
        c3.appendChild(small("Credit Limit"));
        creditLimitLbl.setStyle("display:inline-block; margin:4px 0 10px 0; background:#dbeafe; "
                              + "color:#1d4ed8; font-weight:700; padding:3px 12px; border-radius:999px;");
        c3.appendChild(creditLimitLbl);
        c3.appendChild(small("Payment Term"));
        paymentTermLbl.setStyle("display:block; color:#0f172a; margin-top:4px;");
        c3.appendChild(paymentTermLbl);

        // col 4: site delivery
        Div c4 = new Div();
        c4.setStyle("border-left:1px solid #e2e8f0; padding-left:16px;");
        c4.appendChild(siteDelivery);
        siteAddressBox.setStyle("margin-top:8px;");
        siteAddressBox.appendChild(siteAddress);
        c4.appendChild(siteAddressBox);

        body.appendChild(c1);
        body.appendChild(c2);
        body.appendChild(c3);
        body.appendChild(c4);
        return (Div) body.getParent();
    }

    private Div buildItemsCard() {
        Div body = card("Order Items");
        body.setStyle("");

        Div top = new Div();
        top.setStyle("text-align:right; margin-bottom:8px;");
        top.appendChild(addProductBtn);
        body.appendChild(top);

        Listbox items = new Listbox();
        items.setStyle("width:100%;");
        items.setEmptyMessage("No product added yet. Product line porer step e add hobe.");
        Listhead head = new Listhead();
        String[][] cols = { { "#", "50px" }, { "Product", "180px" }, { "Description", null },
                { "Qty", "90px" }, { "Price", "110px" }, { "Discount", "100px" },
                { "Tax", "90px" }, { "Total", "120px" }, { "Action", "80px" } };
        for (String[] c : cols) {
            Listheader h = new Listheader(c[0]);
            if (c[1] != null)
                h.setWidth(c[1]);
            else
                h.setHflex("1");
            head.appendChild(h);
        }
        items.appendChild(head);
        body.appendChild(items);

        // totals box
        Div totals = new Div();
        totals.setStyle("display:grid; grid-template-columns:1fr 140px; gap:4px 12px; "
                      + "width:300px; margin:14px 0 0 auto; text-align:right;");
        totals.appendChild(new Label("Subtotal"));
        totals.appendChild(subtotalLbl);
        totals.appendChild(new Label("Discount"));
        totals.appendChild(discountLbl);
        totals.appendChild(new Label("VAT / Tax"));
        totals.appendChild(vatLbl);
        Label gt = new Label("Grand Total");
        gt.setStyle("font-weight:700; font-size:15px; color:#15803d;");
        grandLbl.setStyle("font-weight:700; font-size:15px; color:#15803d;");
        totals.appendChild(gt);
        totals.appendChild(grandLbl);
        body.appendChild(totals);
        return (Div) body.getParent();
    }

    private Div buildActionBar() {
        Div bar = new Div();
        bar.setStyle("text-align:right; padding:12px 20px;");
        bar.appendChild(saveBtn);
        bar.appendChild(completeBtn);
        bar.appendChild(printBtn);
        bar.appendChild(cancelBtn);
        return bar;
    }

    /** Creates a card with a title; returns the BODY div (card = body.getParent()). */
    private Div card(String title) {
        Div card = new Div();
        card.setStyle(CARD);
        Label t = new Label(title);
        t.setStyle("display:block; font-size:16px; font-weight:700; color:#1d4ed8; "
                 + "margin-bottom:12px;");
        card.appendChild(t);
        Div body = new Div();
        card.appendChild(body);
        return body;
    }

    private Label small(String text) {
        Label l = new Label(text);
        l.setStyle("display:block; font-size:12px; font-weight:600; color:#64748b;");
        return l;
    }

    private void addRow(Div g, String l1, Component c1, String l2, Component c2) {
        g.appendChild(fieldLabel(l1));
        g.appendChild(c1);
        g.appendChild(fieldLabel(l2));
        g.appendChild(c2);
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("font-weight:600; color:#334155;");
        return l;
    }

    // ============================================================== loaders

    private int clientId() {
        return Env.getAD_Client_ID(Env.getCtx());
    }

    private void loadOrgs() {
        fill(orgList,
            "SELECT AD_Org_ID, Name FROM AD_Org WHERE IsActive='Y' AND IsSummary='N' "
          + "AND AD_Client_ID=? AND AD_Org_ID>0 ORDER BY Name",
            true, clientId());
        if (orgList.getItemCount() == 2)
            orgList.setSelectedIndex(1);
        loadWarehouses();
    }

    private void loadWarehouses() {
        int org = selectedId(orgList);
        fill(whList,
            "SELECT M_Warehouse_ID, Name FROM M_Warehouse WHERE IsActive='Y' "
          + "AND AD_Client_ID=? AND AD_Org_ID=? ORDER BY Name",
            true, clientId(), org);
        if (whList.getItemCount() == 2)
            whList.setSelectedIndex(1);
    }

    private void loadTypes() {
        fill(typeList,
            "SELECT C_DocType_ID, Name FROM C_DocType WHERE IsActive='Y' "
          + "AND AD_Client_ID=? AND DocBaseType='SOO' ORDER BY Name",
            true, clientId());
        if (typeList.getItemCount() == 2)
            typeList.setSelectedIndex(1);
    }

    private void loadSegments() {
        fill(segmentList,
            "SELECT XX_Segment_ID, Name FROM XX_Segment WHERE IsActive='Y' "
          + "AND AD_Client_ID=? ORDER BY Name",
            true, clientId());
        loadTerritories();
    }

    private void loadTerritories() {
        int seg = selectedId(segmentList);
        String st = selectedValue(salesTypeList);
        fill(territoryList,
            "SELECT t.XX_Territory_ID, t.Name FROM XX_Territory t "
          + "WHERE t.IsActive='Y' AND t.AD_Client_ID=? AND t.SalesType=? "
          + "AND EXISTS (SELECT 1 FROM XX_SegmentTerritory st "
          + "  WHERE st.XX_Territory_ID=t.XX_Territory_ID AND st.XX_Segment_ID=? "
          + "  AND st.IsActive='Y') ORDER BY t.Name",
            true, clientId(), st, seg);
        loadCustomers();
    }

    private void loadCustomers() {
        int seg = selectedId(segmentList);
        int terr = selectedId(territoryList);
        String st = selectedValue(salesTypeList);

        StringBuilder sql = new StringBuilder(
            "SELECT bp.C_BPartner_ID, bp.Name FROM C_BPartner bp "
          + "WHERE bp.IsActive='Y' AND bp.IsCustomer='Y' AND bp.AD_Client_ID=? "
          + "AND EXISTS (SELECT 1 FROM XX_BPartnerSegment bs "
          + "  WHERE bs.C_BPartner_ID=bp.C_BPartner_ID AND bs.XX_Segment_ID=? AND bs.IsActive='Y') ");
        List<Object> params = new ArrayList<Object>();
        params.add(clientId());
        params.add(seg);

        if (terr > 0) {
            sql.append("AND bp.XX_Territory_ID=? ");
            params.add(terr);
        } else {
            sql.append("AND EXISTS (SELECT 1 FROM XX_Territory t "
                     + "  WHERE t.XX_Territory_ID=bp.XX_Territory_ID AND t.SalesType=?) ");
            params.add(st);
        }
        sql.append("ORDER BY bp.Name");

        fill(customerList, sql.toString(), true, params.toArray());
        clearCustomerInfo();
    }

    private void loadCustomerInfo(int bpId) {
        clearCustomerInfo();
        if (bpId <= 0)
            return;

        custNameLbl.setValue(customerList.getSelectedItem().getLabel());

        addressLbl.setValue(queryString(
            "SELECT COALESCE(l.Address1,'') || CASE WHEN l.City IS NULL THEN '' ELSE ', ' || l.City END "
          + "FROM C_BPartner_Location bl JOIN C_Location l ON l.C_Location_ID=bl.C_Location_ID "
          + "WHERE bl.C_BPartner_ID=? AND bl.IsActive='Y' ORDER BY bl.C_BPartner_Location_ID LIMIT 1",
            bpId));

        String[] u = queryRow(
            "SELECT COALESCE(Name,''), COALESCE(Phone,''), COALESCE(EMail,'') FROM AD_User "
          + "WHERE C_BPartner_ID=? AND IsActive='Y' ORDER BY AD_User_ID LIMIT 1", 3, bpId);
        contactLbl.setValue(u[0]);
        phoneLbl.setValue(u[1]);
        emailLbl.setValue(u[2]);

        String credit = queryString(
            "SELECT to_char(COALESCE(SO_CreditLimit,0),'FM999,999,999,990.00') "
          + "FROM C_BPartner WHERE C_BPartner_ID=?", bpId);
        creditLimitLbl.setValue(TAKA + credit);

        paymentTermLbl.setValue(queryString(
            "SELECT pt.Name FROM C_BPartner bp JOIN C_PaymentTerm pt "
          + "ON pt.C_PaymentTerm_ID=bp.C_PaymentTerm_ID WHERE bp.C_BPartner_ID=?", bpId));
    }

    private void clearCustomerInfo() {
        custNameLbl.setValue("");
        contactLbl.setValue("");
        phoneLbl.setValue("");
        emailLbl.setValue("");
        addressLbl.setValue("");
        creditLimitLbl.setValue("");
        paymentTermLbl.setValue("");
    }

    // ================================================================ events

    @Override
    public void onEvent(Event e) {
        Object t = e.getTarget();
        if (t == orgList) {
            loadWarehouses();
        } else if (t == segmentList || t == salesTypeList) {
            loadTerritories();
        } else if (t == territoryList) {
            loadCustomers();
        } else if (t == customerList) {
            loadCustomerInfo(selectedId(customerList));
        } else if (t == siteDelivery) {
            boolean on = siteDelivery.isChecked();
            siteAddressBox.setVisible(on);
            if (!on)
                siteAddress.setValue("");
        } else if (t == cancelBtn) {
            resetForm();
        } else if (t == saveBtn || t == completeBtn || t == printBtn || t == addProductBtn) {
            Messagebox.show("Later",
                    "Coming next", Messagebox.OK, Messagebox.INFORMATION);
        }
    }

    private void resetForm() {
        orderDate.setValue(new java.util.Date());
        deliveryDate.setValue(null);
        salesTypeList.setSelectedIndex(0);
        siteDelivery.setChecked(false);
        siteAddressBox.setVisible(false);
        siteAddress.setValue("");
        loadOrgs();
        loadTypes();
        loadSegments();
    }

    // =============================================================== utility

    private int selectedId(Listbox lb) {
        try {
            return Integer.parseInt(selectedValue(lb));
        } catch (Exception ex) {
            return 0;
        }
    }

    private String selectedValue(Listbox lb) {
        if (lb.getSelectedItem() == null)
            return "";
        Object v = lb.getSelectedItem().getValue();
        return v == null ? "" : v.toString();
    }

    private void fill(Listbox lb, String sql, boolean blank, Object... params) {
        lb.getItems().clear();
        if (blank)
            lb.appendItem("", "0");
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = DB.prepareStatement(sql, null);
            DB.setParameters(ps, params);
            rs = ps.executeQuery();
            while (rs.next())
                lb.appendItem(rs.getString(2), String.valueOf(rs.getInt(1)));
        } catch (Exception ex) {
            throw new RuntimeException(sql, ex);
        } finally {
            DB.close(rs, ps);
        }
        if (lb.getItemCount() > 0)
            lb.setSelectedIndex(0);
    }

    private String queryString(String sql, Object... params) {
        return queryRow(sql, 1, params)[0];
    }

    private String[] queryRow(String sql, int cols, Object... params) {
        String[] out = new String[cols];
        for (int i = 0; i < cols; i++)
            out[i] = "";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = DB.prepareStatement(sql, null);
            DB.setParameters(ps, params);
            rs = ps.executeQuery();
            if (rs.next()) {
                for (int i = 0; i < cols; i++) {
                    String v = rs.getString(i + 1);
                    out[i] = v == null ? "" : v;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(sql, ex);
        } finally {
            DB.close(rs, ps);
        }
        return out;
    }
}