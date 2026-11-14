package org.mycompany.o2c.ui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.mycompany.o2c.model.Category;
import org.mycompany.o2c.model.Customer;
import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.model.OrderHeader;
import org.mycompany.o2c.model.OrderTotals;
import org.mycompany.o2c.model.Product;
import org.mycompany.o2c.model.SavedOrder;
import org.mycompany.o2c.model.SalesOrderLine;
import org.mycompany.o2c.model.Segment;
import org.mycompany.o2c.model.StockInfo;
import org.mycompany.o2c.model.Warehouse;
import org.mycompany.o2c.service.CategoryService;
import org.mycompany.o2c.service.CustomerService;
import org.mycompany.o2c.service.InventoryService;
import org.mycompany.o2c.service.ProductService;
import org.mycompany.o2c.service.SalesOrderService;
import org.mycompany.o2c.service.SegmentService;
import org.mycompany.o2c.service.WarehouseService;
import org.mycompany.o2c.util.ValidationException;
import org.mycompany.o2c.util.ValidationUtils;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Textbox;

/**
 * Sales Order form (UI layer only).
 * Builds the ZK components, handles events and shows what the services return.
 * No SQL and no business calculation in this class: see the service / dao packages.
 */
public class WSalesOrderForm extends ADForm implements EventListener<Event> {
    private static final long serialVersionUID = 1L;

    private static final String CARD =
        "background:#ffffff; border:1px solid #e2e8f0; border-radius:12px; "
      + "box-shadow:0 1px 3px rgba(15,23,42,0.08); margin:12px; padding:16px 20px;";
    private static final String INPUT =
        "width:100%; padding:6px 8px; border:1px solid #cbd5e1; border-radius:8px; "
      + "background:#ffffff; box-sizing:border-box;";
    private static final String TAKA = "৳ ";
    private static final String SUBBOX =
        "background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; padding:10px 12px;";

    // ---- services (business logic) -- plain objects, created once per form
    private final WarehouseService warehouseService = new WarehouseService();
    private final CustomerService customerService = new CustomerService();
    private final SegmentService segmentService = new SegmentService();
    private final CategoryService categoryService = new CategoryService();
    private final ProductService productService = new ProductService();
    private final InventoryService inventoryService = new InventoryService();
    private final SalesOrderService salesOrderService = new SalesOrderService();

    // ---- order header
    private Listbox orgList = new Listbox();
    private Listbox whList = new Listbox();
    private Listbox typeList = new Listbox();
    private Listbox salesTypeList = new Listbox();
    private Listbox territoryList = new Listbox();
    private Listbox customerList = new Listbox();
    private Datebox orderDate = new Datebox();
    private Datebox deliveryDate = new Datebox();
    private Listbox priceListList = new Listbox();
    private Listbox salesRepList = new Listbox();
    private Listbox paymentTermList = new Listbox();
    private Textbox notesBox = new Textbox();
    private Label docNoLbl = new Label("Assigned on save");

    // ---- customer card
    private Label custNameLbl = new Label("");
    private Label binLbl = new Label("");
    private Label vatRateLbl = new Label("");
    private Label creditLimitLbl = new Label("");
    private Label paymentTermLbl = new Label("");
    private Label creditDaysLbl = new Label("");
    private Label contactLbl = new Label("");
    private Label segmentsLbl = new Label("");
    private Label addressTitle = new Label("Delivery Address");
    private Label addressLbl = new Label("");
    private Checkbox siteDelivery = new Checkbox("Site Delivery");
    private Textbox siteAddress = new Textbox();
    private Div siteAddressBox = new Div();

    // ---- add product panel
    private Listbox segmentList = new Listbox();
    private Listbox categoryList = new Listbox();
    private Listbox productList = new Listbox();
    private Label segWarnLbl = new Label("");
    private Label pCodeLbl = new Label("");
    private Label pNameLbl = new Label("");
    private Label pUomLbl = new Label("");
    private Label pPriceLbl = new Label("");
    private Label pTaxLbl = new Label("");
    private Label stOnHandLbl = new Label("");
    private Label stReservedLbl = new Label("");
    private Label stAvailLbl = new Label("");
    private Label stReqLbl = new Label("");
    private Label stShortLbl = new Label("");
    private Decimalbox qtyBox = new Decimalbox();
    private Label qtyUomLbl = new Label("");
    private Decimalbox spDiscBox = new Decimalbox();
    private Button addBtn = new Button("+ Add to Order");
    private Button clearBtn = new Button("Clear");

    // product / stock currently shown in the panel
    private Product curProduct = null;
    private StockInfo curStock = StockInfo.EMPTY;

    // ---- order items
    private Listbox items = new Listbox();
    private List<SalesOrderLine> lines = new ArrayList<SalesOrderLine>();

    // ---- totals
    private Label subtotalLbl = new Label(TAKA + "0.00");
    private Label discountLbl = new Label(TAKA + "0.00");
    private Label vatLbl = new Label(TAKA + "0.00");
    private Label grandLbl = new Label(TAKA + "0.00");

    // ---- action buttons
    private Button saveBtn = new Button("Save Draft");
    private Button completeBtn = new Button("Complete Order");
    private Button printBtn = new Button("Print Order");
    private Button cancelBtn = new Button("Cancel");
    private Label statusBadge = new Label("Draft (not saved)");

    /** Set once the Draft is stored; blocks a second save of the same screen. */
    private SavedOrder savedOrder = null;
    private boolean saving = false;

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

        North north = new North();
        ZKUpdateUtil.setVflex(north, "min");
        north.setStyle("background:transparent; border:none;");
        main.appendChild(north);
        north.appendChild(buildTitleBar());

        Center center = new Center();
        center.setAutoscroll(true);
        center.setStyle("background:transparent; border:none;");
        main.appendChild(center);

        Div page = new Div();
        center.appendChild(page);

        // top row: order info (left) + customer info (right)
        Div top = new Div();
        top.setStyle("display:grid; grid-template-columns:repeat(auto-fit, minmax(420px, 1fr)); "
                   + "align-items:stretch;");
        top.appendChild(buildOrderInfoCard());
        top.appendChild(buildCustomerCard());
        page.appendChild(top);

        page.appendChild(buildAddProductCard());
        page.appendChild(buildItemsCard());

        salesTypeList.appendItem("Local", CustomerService.LOCAL);
        salesTypeList.appendItem("Foreign", CustomerService.FOREIGN);
        salesTypeList.setSelectedIndex(0);
        siteAddressBox.setVisible(false);

        loadOrgs();
        loadTypes();
        loadHeaderLookups();
        loadTerritories();
        refreshLines();
    }

    // ================================================================== UI

    private void styleInputs() {
        for (Listbox lb : new Listbox[] { orgList, whList, typeList, salesTypeList,
                territoryList, customerList, segmentList, categoryList, productList,
                priceListList, salesRepList, paymentTermList }) {
            lb.setMold("select");
            lb.setStyle(INPUT);
            lb.addEventListener(Events.ON_SELECT, this);
        }
        orderDate.setFormat("dd-MMM-yyyy");
        deliveryDate.setFormat("dd-MMM-yyyy");
        orderDate.setStyle("width:100%;");
        deliveryDate.setStyle("width:100%;");
        orderDate.setValue(new java.util.Date());

        notesBox.setRows(2);
        notesBox.setStyle(INPUT);
        notesBox.setPlaceholder("Order notes");
        docNoLbl.setStyle("font-weight:700; color:#0f172a;");

        siteDelivery.addEventListener(Events.ON_CHECK, this);
        siteAddress.setRows(3);
        siteAddress.setStyle(INPUT);
        siteAddress.setPlaceholder("Site delivery address");

        qtyBox.setFormat("#,##0.##");
        qtyBox.setStyle("width:140px; padding:6px 8px; border:1px solid #cbd5e1; border-radius:8px;");
        qtyBox.addEventListener(Events.ON_CHANGE, this);

        spDiscBox.setFormat("#,##0.##");
        spDiscBox.setStyle("width:110px; padding:6px 8px; border:1px solid #cbd5e1; border-radius:8px;");
        spDiscBox.setPlaceholder("0");

        btn(saveBtn, "#2563eb", "#ffffff", null);
        btn(completeBtn, "#16a34a", "#ffffff", null);
        btn(printBtn, "#ffffff", "#0f172a", "#cbd5e1");
        btn(cancelBtn, "#fef2f2", "#dc2626", "#fecaca");
        btn(addBtn, "#2563eb", "#ffffff", null);
        btn(clearBtn, "#ffffff", "#334155", "#cbd5e1");
    }

    private void btn(Button b, String bg, String fg, String border) {
        b.setStyle("background:" + bg + "; color:" + fg + "; font-weight:600; "
                 + "padding:8px 18px; border-radius:8px; margin-left:8px; "
                 + "border:" + (border == null ? "none" : "1px solid " + border) + ";");
        b.addEventListener(Events.ON_CLICK, this);
    }

    private Div buildTitleBar() {
        Div bar = new Div();
        bar.setStyle("display:flex; flex-wrap:wrap; justify-content:space-between; "
                   + "align-items:center; gap:8px; margin:12px 12px 0 12px; padding:14px 20px; "
                   + "background:#ffffff; border:1px solid #e2e8f0; border-radius:12px;");

        Div left = new Div();
        Label title = new Label("Sales Order Primitek");
        title.setStyle("display:block; font-size:22px; font-weight:700; color:#0f172a;");
        Label sub = new Label("Create and manage sales orders");
        sub.setStyle("display:block; color:#64748b; font-size:13px; margin-top:2px;");
        left.appendChild(title);
        left.appendChild(sub);

        Div right = new Div();
        right.setStyle("display:flex; align-items:center; flex-wrap:wrap;");
        Label badge = statusBadge;
        badge.setStyle("background:#dbeafe; color:#1d4ed8; font-weight:600; "
                     + "padding:5px 18px; border-radius:999px; margin-right:8px;");
        right.appendChild(badge);
        right.appendChild(saveBtn);
        right.appendChild(completeBtn);
        right.appendChild(printBtn);
        right.appendChild(cancelBtn);

        bar.appendChild(left);
        bar.appendChild(right);
        return bar;
    }

    private Div buildOrderInfoCard() {
        Div body = card("Order Information");
        body.setStyle("display:grid; grid-template-columns:130px 1fr 130px 1fr; "
                    + "gap:10px 16px; align-items:center;");
        addRow(body, "Organization *", orgList, "Depot / Warehouse *", whList);
        addRow(body, "Order Date *", orderDate, "Expected Delivery", deliveryDate);
        addRow(body, "Type of Order", typeList, "Sales Type", salesTypeList);
        addRow(body, "Territory", territoryList, "Customer *", customerList);
        addRow(body, "Price List *", priceListList, "Sales Rep", salesRepList);
        addRow(body, "Payment Term", paymentTermList, "Document No", docNoLbl);
        body.appendChild(fieldLabel("Notes"));
        notesBox.setHflex("1");
        Div notesCell = new Div();
        notesCell.setStyle("grid-column:span 3;");
        notesCell.appendChild(notesBox);
        body.appendChild(notesCell);
        return (Div) body.getParent();
    }

    private Div buildCustomerCard() {
        Div body = card("Customer Information");
        body.setStyle("");

        Div kv = new Div();
        kv.setStyle("display:grid; grid-template-columns:110px 1fr; gap:4px 10px;");
        custNameLbl.setStyle("font-weight:700; color:#0f172a;");
        kvRow(kv, "Customer:", custNameLbl);
        kvRow(kv, "BIN / Tax ID:", binLbl);
        kvRow(kv, "VAT Rate:", vatRateLbl);
        kvRow(kv, "Credit Limit:", creditLimitLbl);
        kvRow(kv, "Payment Term:", paymentTermLbl);
        kvRow(kv, "Credit Days:", creditDaysLbl);
        kvRow(kv, "Contact:", contactLbl);
        kvRow(kv, "Segments:", segmentsLbl);
        body.appendChild(kv);

        // Site Delivery: tick dile Delivery Address er jayga e site address textbox
        Div addr = new Div();
        addr.setStyle(SUBBOX + " margin-top:10px;");
        siteDelivery.setStyle("font-weight:600; color:#0f172a; display:block; margin-bottom:6px;");
        addr.appendChild(siteDelivery);
        addressTitle.setStyle("display:block; font-size:12px; font-weight:600; color:#64748b;");
        addr.appendChild(addressTitle);
        addressLbl.setStyle("display:block; color:#0f172a; margin-top:2px;");
        addr.appendChild(addressLbl);
        siteAddressBox.appendChild(siteAddress);
        addr.appendChild(siteAddressBox);
        body.appendChild(addr);
        return (Div) body.getParent();
    }

    private Div buildAddProductCard() {
        Div body = card("Add Product");
        body.setStyle("");

        Div row1 = new Div();
        row1.setStyle("display:grid; grid-template-columns:repeat(auto-fit, minmax(210px, 1fr)); gap:12px;");
        row1.appendChild(pickCell("Segment *", segmentList));
        row1.appendChild(pickCell("Category *", categoryList));
        row1.appendChild(pickCell("Product *", productList));

        Div details = new Div();
        details.setStyle(SUBBOX + " background:#eff6ff;");
        Label dt = new Label("Product Details");
        dt.setStyle("display:block; font-weight:700; color:#1d4ed8; margin-bottom:4px;");
        details.appendChild(dt);
        Div dkv = new Div();
        dkv.setStyle("display:grid; grid-template-columns:70px 1fr; gap:2px 8px;");
        kvRow(dkv, "Code:", pCodeLbl);
        kvRow(dkv, "Name:", pNameLbl);
        kvRow(dkv, "UOM:", pUomLbl);
        kvRow(dkv, "Price:", pPriceLbl);
        kvRow(dkv, "Tax:", pTaxLbl);
        details.appendChild(dkv);
        row1.appendChild(details);

        Div stock = new Div();
        stock.setStyle(SUBBOX + " background:#f0fdf4;");
        Label st = new Label("Stock (selected warehouse)");
        st.setStyle("display:block; font-weight:700; color:#15803d; margin-bottom:4px;");
        stock.appendChild(st);
        Div skv = new Div();
        skv.setStyle("display:grid; grid-template-columns:90px 1fr; gap:2px 8px;");
        kvRow(skv, "On Hand:", stOnHandLbl);
        kvRow(skv, "Reserved:", stReservedLbl);
        kvRow(skv, "Available:", stAvailLbl);
        kvRow(skv, "Requested:", stReqLbl);
        kvRow(skv, "Shortage:", stShortLbl);
        stock.appendChild(skv);
        row1.appendChild(stock);
        body.appendChild(row1);

        segWarnLbl.setStyle("display:block; color:#b45309; margin-top:6px;");
        body.appendChild(segWarnLbl);

        // NOTE: category-wise Product Attributes row will be added in Stage 2.

        Div row2 = new Div();
        row2.setStyle("display:flex; flex-wrap:wrap; align-items:center; gap:12px; margin-top:12px;");
        Label ql = new Label("Order Quantity *");
        ql.setStyle("font-weight:600; color:#334155;");
        qtyUomLbl.setStyle("color:#64748b;");
        row2.appendChild(ql);
        row2.appendChild(qtyBox);
        row2.appendChild(qtyUomLbl);
        Label sl = new Label("Special Discount (%)");
        sl.setStyle("font-weight:600; color:#334155; margin-left:24px;");
        row2.appendChild(sl);
        row2.appendChild(spDiscBox);
        body.appendChild(row2);

        Div row3 = new Div();
        row3.setStyle("text-align:right; margin-top:10px;");
        row3.appendChild(addBtn);
        row3.appendChild(clearBtn);
        body.appendChild(row3);
        return (Div) body.getParent();
    }

    private Div pickCell(String title, Listbox lb) {
        Div d = new Div();
        d.setStyle(SUBBOX);
        Label l = new Label(title);
        l.setStyle("display:block; font-weight:600; color:#334155; margin-bottom:4px;");
        d.appendChild(l);
        d.appendChild(lb);
        return d;
    }

    private void kvRow(Div g, String k, Label v) {
        Label kl = new Label(k);
        kl.setStyle("color:#64748b; font-size:12px;");
        if (v.getStyle() == null || v.getStyle().isEmpty())
            v.setStyle("color:#0f172a;");
        g.appendChild(kl);
        g.appendChild(v);
    }

    private Div buildItemsCard() {
        Div body = card("Order Items");
        body.setStyle("");

        items.setStyle("width:100%;");
        items.setEmptyMessage("No product added yet.");
        Listhead head = new Listhead();
        String[][] cols = { { "#", "40px" }, { "Segment", "110px" }, { "Category", "110px" },
                { "Product", "170px" }, { "Description", null }, { "UOM", "60px" },
                { "Qty", "80px" }, { "Price", "100px" }, { "Gross", "110px" },
                { "Special Disc.", "120px" }, { "VAT", "100px" }, { "Total", "120px" }, { "Action", "130px" } };
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

        Div totals = new Div();
        totals.setStyle("display:grid; grid-template-columns:1fr 140px; gap:4px 12px; "
                      + "width:300px; margin:14px 0 0 auto; text-align:right;");
        totals.appendChild(new Label("Subtotal (Gross)"));
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
    // Each loader asks a service for model objects and fills the components.

    private void loadOrgs() {
        fill(orgList, warehouseService.getOrganizations(), Option::getId, Option::getName);
        if (orgList.getItemCount() == 2)
            orgList.setSelectedIndex(1);
        loadWarehouses();
    }

    private void loadWarehouses() {
        fill(whList, warehouseService.getWarehouses(selectedId(orgList)),
                Warehouse::getId, Warehouse::getName);
        if (whList.getItemCount() == 2)
            whList.setSelectedIndex(1);
        refreshStock();
    }

    private void loadTypes() {
        fill(typeList, salesOrderService.getOrderTypes(), Option::getId, Option::getName);
        if (typeList.getItemCount() == 2)
            typeList.setSelectedIndex(1);
    }

    private void loadHeaderLookups() {
        fill(priceListList, salesOrderService.getPriceLists(), Option::getId, Option::getName);
        if (priceListList.getItemCount() == 2)
            priceListList.setSelectedIndex(1);
        fill(salesRepList, salesOrderService.getSalesReps(), Option::getId, Option::getName);
        fill(paymentTermList, salesOrderService.getPaymentTerms(), Option::getId, Option::getName);
    }

    /** Territory: Sales Type onujayi. */
    private void loadTerritories() {
        fill(territoryList, customerService.getTerritories(selectedValue(salesTypeList)),
                Option::getId, Option::getName);
        loadCustomers();
    }

    /** Customer: Sales Type + Territory (blank = National). */
    private void loadCustomers() {
        fill(customerList,
                customerService.getCustomers(selectedId(territoryList), selectedValue(salesTypeList)),
                Customer::getId, Customer::getName);
        lines.clear();
        refreshLines();
        clearCustomerInfo();
    }

    private void loadCustomerInfo(int bpId) {
        clearCustomerInfo();
        Customer c = customerService.getCustomerDetails(bpId);
        if (c == null)
            return;

        custNameLbl.setValue(c.getName());
        addressLbl.setValue(c.getAddress());
        contactLbl.setValue(c.getContactPhone().isEmpty() ? c.getContactName()
                : c.getContactName() + " (" + c.getContactPhone() + ")");
        binLbl.setValue(c.getTaxId());
        creditLimitLbl.setValue(TAKA + fmt(c.getCreditLimit()));
        paymentTermLbl.setValue(c.getPaymentTerm());
        selectById(paymentTermList, salesOrderService.getCustomerPaymentTerm(bpId));
        creditDaysLbl.setValue(c.getCreditDays() + " days");
        vatRateLbl.setValue(fmt(c.getVatRate()) + " %");

        // customer er segment gulo = Segment dropdown er option
        loadSegments(bpId);
    }

    private void clearCustomerInfo() {
        custNameLbl.setValue("");
        binLbl.setValue("");
        vatRateLbl.setValue("");
        creditLimitLbl.setValue("");
        paymentTermLbl.setValue("");
        creditDaysLbl.setValue("");
        contactLbl.setValue("");
        segmentsLbl.setValue("");
        addressLbl.setValue("");
        segWarnLbl.setValue("");
        clearList(segmentList);
        clearList(categoryList);
        clearList(productList);
        clearProductInfo();
        qtyBox.setValue((BigDecimal) null);
        spDiscBox.setValue((BigDecimal) null);
    }

    // ---- Segment -> Category -> Product

    private void loadSegments(int bpId) {
        List<Segment> segments = segmentService.getCustomerSegments(bpId);
        fill(segmentList, segments, Segment::getId, Segment::getName);

        StringBuilder names = new StringBuilder();
        for (Segment s : segments) {
            if (names.length() > 0)
                names.append(", ");
            names.append(s.getName());
        }
        segmentsLbl.setValue(names.toString());
        segWarnLbl.setValue(segments.isEmpty()
                ? "This customer has no segment assigned. Assign a segment to add products." : "");
        loadCategories();
    }

    private void loadCategories() {
        fill(categoryList, categoryService.getSegmentCategories(selectedId(segmentList)),
                Category::getId, Category::getName);
        loadProducts();
    }

    private void loadProducts() {
        fill(productList, productService.getCategoryProducts(selectedId(categoryList)),
                Product::getId, Product::getName);
        clearProductInfo();
    }

    private void clearProductInfo() {
        curProduct = null;
        curStock = StockInfo.EMPTY;
        pCodeLbl.setValue("");
        pNameLbl.setValue("");
        pUomLbl.setValue("");
        pPriceLbl.setValue("");
        pTaxLbl.setValue("");
        qtyUomLbl.setValue("");
        stOnHandLbl.setValue("");
        stReservedLbl.setValue("");
        stAvailLbl.setValue("");
        stReqLbl.setValue("");
        stShortLbl.setValue("");
        stShortLbl.setStyle("color:#0f172a;");
    }

    private void loadProductInfo(int prodId) {
        clearProductInfo();
        Product p = productService.getProductInfo(prodId, selectedId(categoryList),
                selectedId(customerList), orderDate.getValue());
        if (p == null)
            return;
        curProduct = p;

        pCodeLbl.setValue(p.getCode());
        pNameLbl.setValue(p.getName());
        pUomLbl.setValue(p.getUom());
        qtyUomLbl.setValue(p.getUom());
        pPriceLbl.setValue(p.getPrice() == null ? "No price in Price List" : TAKA + fmt(p.getPrice()));
        pTaxLbl.setValue(fmt(p.getTaxRate()) + " %");

        refreshStock();
    }

    /** Stock of the selected product in the selected warehouse (read only). */
    private void refreshStock() {
        curStock = inventoryService.getStock(selectedId(productList), selectedId(whList));
        if (selectedId(productList) <= 0 || selectedId(whList) <= 0) {
            stOnHandLbl.setValue("");
            stReservedLbl.setValue("");
            stAvailLbl.setValue("");
            stReqLbl.setValue("");
            stShortLbl.setValue("");
            return;
        }
        stOnHandLbl.setValue(fmt(curStock.getOnHand()));
        stReservedLbl.setValue(fmt(curStock.getReserved()));
        stAvailLbl.setValue(fmt(curStock.getAvailable()));
        updateRequested();
    }

    private void updateRequested() {
        BigDecimal q = qtyBox.getValue();
        if (q == null || selectedId(productList) <= 0) {
            stReqLbl.setValue("");
            stShortLbl.setValue("");
            return;
        }
        stReqLbl.setValue(fmt(q));
        BigDecimal shortage = curStock.shortageFor(q);
        if (shortage.signum() > 0) {
            stShortLbl.setValue(fmt(shortage) + "  (warning)");
            stShortLbl.setStyle("color:#dc2626; font-weight:700;");
        } else {
            stShortLbl.setValue("0");
            stShortLbl.setStyle("color:#0f172a;");
        }
    }

    // ============================================================ order lines

    private void addLine() {
        if (lockedAfterSave())
            return;
        try {
            SalesOrderLine line = salesOrderService.buildLine(
                    selectedId(customerList), selectedId(whList),
                    selectedId(segmentList), selectedLabel(segmentList),
                    selectedId(categoryList), selectedLabel(categoryList),
                    selectedId(productList), curProduct, qtyBox.getValue(),
                    spDiscBox.getValue(), lines);
            lines.add(line);
        } catch (ValidationException ex) {
            warn(ex.getMessage());
            return;
        }
        refreshLines();
        clearPanel();
    }

    private void editLine(int idx) {
        if (idx < 0 || idx >= lines.size() || lockedAfterSave())
            return;
        SalesOrderLine l = lines.remove(idx);
        refreshLines();

        selectById(segmentList, l.getSegmentId());
        loadCategories();
        selectById(categoryList, l.getCategoryId());
        loadProducts();
        selectById(productList, l.getProductId());
        loadProductInfo(l.getProductId());
        qtyBox.setValue(l.getQty());
        spDiscBox.setValue(l.getSpecialDiscountPct().signum() > 0 ? l.getSpecialDiscountPct() : null);
        updateRequested();
    }

    private void removeLine(int idx) {
        if (idx < 0 || idx >= lines.size() || lockedAfterSave())
            return;
        lines.remove(idx);
        refreshLines();
    }

    private void clearPanel() {
        if (segmentList.getItemCount() > 0)
            segmentList.setSelectedIndex(0);
        loadCategories();
        qtyBox.setValue((BigDecimal) null);
        spDiscBox.setValue((BigDecimal) null);
        stReqLbl.setValue("");
        stShortLbl.setValue("");
    }

    private void refreshLines() {
        items.getItems().clear();
        int n = 0;
        for (SalesOrderLine l : lines) {
            Listitem li = new Listitem();
            li.appendChild(new Listcell(String.valueOf(++n)));
            li.appendChild(new Listcell(l.getSegmentName()));
            li.appendChild(new Listcell(l.getCategoryName()));
            li.appendChild(new Listcell(l.getName()));
            li.appendChild(new Listcell(l.getDescription()));
            li.appendChild(new Listcell(l.getUom()));
            li.appendChild(new Listcell(fmt(l.getQty())));
            li.appendChild(new Listcell(TAKA + fmt(l.getPrice())));
            li.appendChild(new Listcell(TAKA + fmt(l.getGross())));
            li.appendChild(new Listcell(l.getDiscount().signum() > 0
                    ? TAKA + fmt(l.getDiscount()) + " (" + fmt(l.getSpecialDiscountPct()) + "%)"
                    : "-"));
            li.appendChild(new Listcell(TAKA + fmt(l.getVat()) + " (" + fmt(l.getTaxRate()) + "%)"));
            li.appendChild(new Listcell(TAKA + fmt(l.getTotal())));

            Listcell act = new Listcell();
            Button e = new Button("Edit");
            e.setAttribute("act", "edit");
            e.setAttribute("idx", Integer.valueOf(n - 1));
            e.addEventListener(Events.ON_CLICK, this);
            Button r = new Button("Remove");
            r.setAttribute("act", "remove");
            r.setAttribute("idx", Integer.valueOf(n - 1));
            r.addEventListener(Events.ON_CLICK, this);
            act.appendChild(e);
            act.appendChild(r);
            li.appendChild(act);
            items.appendChild(li);
        }
        OrderTotals t = salesOrderService.calculateTotals(lines);
        subtotalLbl.setValue(TAKA + fmt(t.getSubtotal()));
        discountLbl.setValue(TAKA + fmt(t.getDiscount()));
        vatLbl.setValue(TAKA + fmt(t.getVat()));
        grandLbl.setValue(TAKA + fmt(t.getGrandTotal()));
    }

    // ================================================================ events

    @Override
    public void onEvent(Event e) {
        Object t = e.getTarget();

        if (t instanceof Component) {
            Object act = ((Component) t).getAttribute("act");
            if (act != null) {
                int idx = ((Integer) ((Component) t).getAttribute("idx")).intValue();
                if ("edit".equals(act))
                    editLine(idx);
                else if ("remove".equals(act))
                    removeLine(idx);
                return;
            }
        }

        if (t == orgList) {
            loadWarehouses();
        } else if (t == whList) {
            refreshStock();
        } else if (t == salesTypeList) {
            loadTerritories();
        } else if (t == territoryList) {
            loadCustomers();
        } else if (t == customerList) {
            if (!lines.isEmpty()) {
                lines.clear();
                refreshLines();
                warn("Customer changed. Order lines were cleared.");
            }
            loadCustomerInfo(selectedId(customerList));
        } else if (t == segmentList) {
            loadCategories();
        } else if (t == categoryList) {
            loadProducts();
        } else if (t == productList) {
            loadProductInfo(selectedId(productList));
        } else if (t == qtyBox) {
            updateRequested();
        } else if (t == addBtn) {
            addLine();
        } else if (t == clearBtn) {
            clearPanel();
        } else if (t == siteDelivery) {
            applySiteDelivery(siteDelivery.isChecked());
        } else if (t == cancelBtn) {
            resetForm();
        } else if (t == saveBtn) {
            saveDraft();
        } else if (t == completeBtn || t == printBtn) {
            Messagebox.show("Later",
                    "Coming next", Messagebox.OK, Messagebox.INFORMATION);
        }
    }

    // ================================================================ save

    private OrderHeader readHeader() {
        OrderHeader h = new OrderHeader();
        h.setOrgId(selectedId(orgList));
        h.setWarehouseId(selectedId(whList));
        h.setDocTypeId(selectedId(typeList));
        h.setBpartnerId(selectedId(customerList));
        h.setPriceListId(selectedId(priceListList));
        h.setSalesRepId(selectedId(salesRepList));
        h.setPaymentTermId(selectedId(paymentTermList));
        h.setDateOrdered(toTimestamp(orderDate.getValue()));
        h.setDatePromised(toTimestamp(deliveryDate.getValue()));
        h.setDescription(notesBox.getValue());
        h.setSalesType(selectedValue(salesTypeList));
        h.setTerritoryId(selectedId(territoryList));
        h.setSiteDelivery(siteDelivery.isChecked());
        h.setSiteAddress(siteAddress.getValue());
        return h;
    }

    private java.sql.Timestamp toTimestamp(java.util.Date d) {
        return d == null ? null : new java.sql.Timestamp(d.getTime());
    }

    /** Save Draft: one click = one transaction. A second save of the same screen is refused. */
    private void saveDraft() {
        if (savedOrder != null) {
            warn("This order is already saved as " + savedOrder.getDocumentNo()
                    + ". Press New Order to start another one.");
            return;
        }
        if (saving)
            return;
        saving = true;
        saveBtn.setDisabled(true);
        try {
            SavedOrder so = salesOrderService.saveDraft(readHeader(), lines);
            savedOrder = so;
            docNoLbl.setValue(so.getDocumentNo());
            statusBadge.setValue("Draft saved: " + so.getDocumentNo());
            cancelBtn.setLabel("New Order");
            addBtn.setDisabled(true);
            Messagebox.show("Draft saved.\nDocument No: " + so.getDocumentNo()
                    + "\nStatus: " + so.getDocStatus()
                    + "\nLines total: " + TAKA + fmt(so.getTotalLines())
                    + "\nGrand total (iDempiere): " + TAKA + fmt(so.getGrandTotal()),
                    "Sales Order", Messagebox.OK, Messagebox.INFORMATION);
        } catch (ValidationException ex) {
            saveBtn.setDisabled(false);
            warn(ex.getMessage());
        } catch (RuntimeException ex) {
            saveBtn.setDisabled(false);
            warn("Could not save the order. Nothing was stored.\n" + ex.getMessage());
        } finally {
            saving = false;
        }
    }

    private boolean lockedAfterSave() {
        if (savedOrder == null)
            return false;
        warn("The order is saved as " + savedOrder.getDocumentNo()
                + ". Editing a saved Draft is not available yet. Press New Order.");
        return true;
    }

    /** Site Delivery on: customer address er jayga e site address textbox. */
    private void applySiteDelivery(boolean on) {
        addressTitle.setValue(on ? "Site Delivery Address" : "Delivery Address");
        addressLbl.setVisible(!on);
        siteAddressBox.setVisible(on);
        if (!on)
            siteAddress.setValue("");
    }

    private void resetForm() {
        savedOrder = null;
        saveBtn.setDisabled(false);
        addBtn.setDisabled(false);
        cancelBtn.setLabel("Cancel");
        statusBadge.setValue("Draft (not saved)");
        docNoLbl.setValue("Assigned on save");
        notesBox.setValue("");
        orderDate.setValue(new java.util.Date());
        deliveryDate.setValue(null);
        salesTypeList.setSelectedIndex(0);
        siteDelivery.setChecked(false);
        applySiteDelivery(false);
        loadOrgs();
        loadTypes();
        loadHeaderLookups();
        loadTerritories();
    }

    private void warn(String msg) {
        Messagebox.show(msg, "Sales Order", Messagebox.OK, Messagebox.EXCLAMATION);
    }

    // =============================================================== utility

    private String fmt(BigDecimal v) {
        return new DecimalFormat("#,##0.00").format(v == null ? BigDecimal.ZERO : v);
    }

    private int selectedId(Listbox lb) {
        return ValidationUtils.parseId(selectedValue(lb));
    }

    private String selectedValue(Listbox lb) {
        if (lb.getSelectedItem() == null)
            return "";
        Object v = lb.getSelectedItem().getValue();
        return v == null ? "" : v.toString();
    }

    private String selectedLabel(Listbox lb) {
        return lb.getSelectedItem() == null ? "" : lb.getSelectedItem().getLabel();
    }

    private void selectById(Listbox lb, int id) {
        String v = String.valueOf(id);
        for (Listitem li : lb.getItems()) {
            if (v.equals(li.getValue())) {
                lb.setSelectedItem(li);
                return;
            }
        }
    }

    private void clearList(Listbox lb) {
        lb.getItems().clear();
        lb.appendItem("", "0");
        lb.setSelectedIndex(0);
    }

    /** Blank first item, then one item per model object; first item selected. */
    private <T> void fill(Listbox lb, List<T> data, Function<T, Integer> id, Function<T, String> name) {
        lb.getItems().clear();
        lb.appendItem("", "0");
        for (T t : data)
            lb.appendItem(name.apply(t), String.valueOf(id.apply(t)));
        lb.setSelectedIndex(0);
    }
}
