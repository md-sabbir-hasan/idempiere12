package org.mycompany.o2c.ui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.mycompany.o2c.model.Category;
import org.mycompany.o2c.model.IdName;
import org.mycompany.o2c.model.Customer;
import org.mycompany.o2c.model.OfficerScope;
import org.mycompany.o2c.model.OfficerScope.Territory;
import org.mycompany.o2c.model.Option;
import org.mycompany.o2c.model.OrderDetail;
import org.mycompany.o2c.model.OrderHeader;
import org.mycompany.o2c.model.OrderRow;
import org.mycompany.o2c.model.OrderTotals;
import org.mycompany.o2c.model.Product;
import org.mycompany.o2c.model.SavedOrder;
import org.mycompany.o2c.model.SubmitResult;
import org.mycompany.o2c.model.ZoneManager;
import org.mycompany.o2c.model.SalesOrderLine;
import org.mycompany.o2c.model.StockInfo;
import org.mycompany.o2c.service.CategoryService;
import org.mycompany.o2c.service.CustomerService;
import org.mycompany.o2c.service.InventoryService;
import org.mycompany.o2c.service.OfficerScopeService;
import org.mycompany.o2c.service.OrderListService;
import org.mycompany.o2c.service.ProductService;
import org.mycompany.o2c.service.SalesOrderService;
import org.mycompany.o2c.service.HeaderLookupService;
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
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
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
    private final OfficerScopeService scopeService = new OfficerScopeService();
    private final OrderListService orderListService = new OrderListService();
    private final HeaderLookupService headerLookupService = new HeaderLookupService();
    private final CategoryService categoryService = new CategoryService();
    private final ProductService productService = new ProductService();
    private final InventoryService inventoryService = new InventoryService();
    private final SalesOrderService salesOrderService = new SalesOrderService();

    // ---- order header
    private Listbox orgList = new Listbox();
    private Listbox whList = new Listbox();
    private Listbox salesTypeList = new Listbox();
    private Listbox territoryList = new Listbox();
    private Listbox customerList = new Listbox();
    private Datebox orderDate = new Datebox();
    private Datebox deliveryDate = new Datebox();
    private Listbox priceListList = new Listbox();
    private Label officerLbl = new Label("");
    private Label segmentLbl = new Label("");
    private Listbox locationList = new Listbox();
    private Listbox departmentList = new Listbox();
    private Radiogroup payGroup = new Radiogroup();
    private Radio payCash = new Radio("Cash");
    private Radio payCredit = new Radio("Credit");
    private Radio paySample = new Radio("Sample");
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
    private Label addressTitle = new Label("Delivery Address");
    private Label addressLbl = new Label("");
    private Checkbox siteDelivery = new Checkbox("Site Delivery");
    private Textbox siteAddress = new Textbox();
    private Div siteAddressBox = new Div();

    // ---- add product panel
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
    private Button completeBtn = new Button("Submit for Approval");
    private Button printBtn = new Button("Print Order");
    private Button cancelBtn = new Button("Cancel");
    private Label statusBadge = new Label("Draft (not saved)");

    /** Id of the order on the screen (0 = a new order, not saved yet) and whether it can still be changed (Draft). */
    private int editingOrderId = 0;
    private boolean editable = true;
    private boolean saving = false;
    private final Label titleLbl = new Label("New Sales Order");

    // ---- the officer's own scope: Segment, Sales Types, Territories (from the Officer Assignment)
    private OfficerScope scope = null;
    private String scopeError = null;

    // ---- two pages in one form: the order list, and the create page
    private final Div listPage = new Div();
    private final Div formPage = new Div();
    private final Datebox fFrom = new Datebox();
    private final Datebox fTo = new Datebox();
    private final Textbox fCustomer = new Textbox();
    private final Listbox fStatus = new Listbox();
    private final Listbox orderGrid = new Listbox();
    private final Label listCount = new Label("");
    private final Button createBtn = new Button("+ Create Order");
    private final Button backBtn = new Button("\u2190 Back to list");
    private final Button searchBtn = new Button("Search");
    private final Button resetBtn = new Button("Reset");

    @Override
    protected void initForm() {
        setStyle("background:#f1f5f9;");
        ZKUpdateUtil.setWidth(this, "99%");
        ZKUpdateUtil.setHeight(this, "100%");

        styleInputs();
        loadScope();

        buildListPage();
        buildFormPage();
        appendChild(listPage);
        appendChild(formPage);

        siteAddressBox.setVisible(false);
        loadOrgs();
        loadHeaderLookups();
        applyScope();
        refreshLines();

        refreshOrderList();
        showList();
    }

    /** Reads the logged in officer's assignment. On a problem the message is shown on the list page. */
    private void loadScope() {
        try {
            scope = scopeService.load(headerLookupService.getCurrentUserId(), LocalDate.now());
            scopeError = null;
        } catch (ValidationException ex) {
            scope = null;
            scopeError = ex.getMessage();
        } catch (RuntimeException ex) {
            scope = null;
            scopeError = "Could not read the Officer Assignment: " + ex.getMessage();
        }
    }

    private void showList() {
        listPage.setVisible(true);
        formPage.setVisible(false);
    }

    private void showForm() {
        listPage.setVisible(false);
        formPage.setVisible(true);
    }

    /** Create Order: a fresh form on its own page. */
    private void openCreate() {
        if (scope == null) {
            warn(scopeError == null ? "You have no current Officer Assignment." : scopeError);
            return;
        }
        resetForm();
        showForm();
    }

    private void buildFormPage() {
        ZKUpdateUtil.setHeight(formPage, "100%");

        Borderlayout main = new Borderlayout();
        ZKUpdateUtil.setWidth(main, "100%");
        ZKUpdateUtil.setHeight(main, "100%");
        formPage.appendChild(main);

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
    }

    // ============================================================ list page

    private void buildListPage() {
        listPage.setStyle("height:100%; overflow:auto;");

        // header
        Div bar = new Div();
        bar.setStyle("display:flex; align-items:center; justify-content:space-between; margin:12px 12px 0 12px;");
        Div titles = new Div();
        Label title = new Label("Sales Order");
        title.setStyle("display:block; font-size:22px; font-weight:700; color:#0f172a;");
        Label sub = new Label("Your sales orders. Press Create Order to enter a new one.");
        sub.setStyle("display:block; font-size:13px; color:#64748b; margin-top:2px;");
        titles.appendChild(title);
        titles.appendChild(sub);
        bar.appendChild(titles);
        bar.appendChild(createBtn);
        listPage.appendChild(bar);

        if (scopeError != null) {
            Label notice = new Label(scopeError);
            notice.setStyle("display:block; margin:12px 12px 0 12px; padding:10px 14px; background:#fffbeb; "
                          + "border:1px solid #fcd34d; border-radius:10px; color:#92400e;");
            listPage.appendChild(notice);
        }

        // filters
        Div card = new Div();
        card.setStyle(CARD);
        Div g = new Div();
        g.setStyle("display:grid; grid-template-columns:repeat(auto-fit, minmax(200px, 1fr)); gap:12px 16px;");
        g.appendChild(listField("Order Date From", fFrom));
        g.appendChild(listField("Order Date To", fTo));
        g.appendChild(listField("Customer", fCustomer));
        g.appendChild(listField("Status", fStatus));
        card.appendChild(g);
        Div buttons = new Div();
        buttons.setStyle("margin-top:14px;");
        buttons.appendChild(searchBtn);
        buttons.appendChild(resetBtn);
        card.appendChild(buttons);
        listPage.appendChild(card);

        // list
        Div listCard = new Div();
        listCard.setStyle(CARD);
        listCount.setStyle("display:block; font-size:13px; color:#64748b; margin-bottom:8px;");
        listCard.appendChild(listCount);

        orderGrid.setMold("paging");
        orderGrid.setPageSize(15);
        orderGrid.setEmptyMessage("No orders found.");
        ZKUpdateUtil.setWidth(orderGrid, "100%");
        // Pixel widths (percent widths collapse the columns in a paging Listbox);
        // only Customer stretches, and the table scrolls sideways on a narrow screen.
        orderGrid.setSizedByContent(false);
        orderGrid.setStyle("overflow-x:auto;");
        Listhead head = new Listhead();
        head.setSizable(true);
        String[][] cols = { { "Document No", "140px" }, { "Order Date", "120px" }, { "Customer", null },
                { "Territory", "170px" }, { "Sales Type", "150px" }, { "Segment", "140px" },
                { "Grand Total", "130px" }, { "Status", "260px" } };
        for (String[] c : cols) {
            Listheader h = new Listheader(c[0]);
            if (c[1] != null)
                h.setWidth(c[1]);
            else
                h.setHflex("1");          // hflex only: ZK does not allow hflex and width together
            if ("Grand Total".equals(c[0]))
                h.setAlign("right");
            head.appendChild(h);
        }
        orderGrid.appendChild(head);
        listCard.appendChild(orderGrid);
        listPage.appendChild(listCard);
    }

    private Div listField(String label, Component input) {
        Div d = new Div();
        Label l = new Label(label);
        l.setStyle("display:block; font-size:12px; color:#64748b; margin-bottom:4px;");
        d.appendChild(l);
        d.appendChild(input);
        return d;
    }

    private void resetOrderFilters() {
        fFrom.setValueInLocalDate(LocalDate.now().minusDays(30));
        fTo.setValue(null);
        fCustomer.setValue("");
        fStatus.setSelectedIndex(0);
    }

    private void refreshOrderList() {
        List<OrderRow> rows;
        try {
            rows = orderListService.list(headerLookupService.getCurrentUserId(),
                    fFrom.getValueInLocalDate(), fTo.getValueInLocalDate(),
                    fCustomer.getValue(), selectedValue(fStatus));
        } catch (ValidationException ex) {
            warn(ex.getMessage());
            return;
        } catch (RuntimeException ex) {
            warn("Could not load the order list: " + ex.getMessage());
            return;
        }
        orderGrid.getItems().clear();
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("dd-MMM-yyyy");
        for (OrderRow r : rows) {
            Listitem li = new Listitem();
            li.setAttribute("orderId", Integer.valueOf(r.orderId()));   // click opens the order
            li.setStyle("cursor:pointer;");
            li.addEventListener(Events.ON_CLICK, this);
            Listcell no = new Listcell(r.documentNo());
            no.setStyle("font-weight:600;");
            li.appendChild(no);
            li.appendChild(new Listcell(r.dateOrdered() == null ? "" : df.format(r.dateOrdered())));
            li.appendChild(new Listcell(r.customer()));
            li.appendChild(new Listcell(r.territory()));
            li.appendChild(new Listcell(r.salesType()));
            li.appendChild(new Listcell(r.segment()));
            Listcell gt = new Listcell(TAKA + fmt(r.grandTotal()));
            gt.setStyle("text-align:right; white-space:nowrap;");
            li.appendChild(gt);
            Listcell st = new Listcell();
            Label badge = new Label(statusFor(r.docStatus(), r.waitingFor()));
            badge.setStyle(statusStyle(r.docStatus()));
            st.appendChild(badge);
            li.appendChild(st);
            orderGrid.appendChild(li);
        }
        listCount.setValue(rows.size() + (rows.size() == 1 ? " order" : " orders"));
    }

    private static String statusText(String st) {
        if (st == null)
            return "";
        switch (st) {
            case "DR": return "Draft";
            case "IP": return "In Approval";
            case "CO": return "Completed";
            case "CL": return "Closed";
            case "VO": return "Voided";
            case "RE": return "Reversed";
            case "NA": return "Not Approved";
            default: return st;
        }
    }

    /** Status text; an order in approval also says which step (and person) it waits for. */
    private static String statusFor(String st, String waitingFor) {
        if ("IP".equals(st) && waitingFor != null && !waitingFor.isEmpty())
            return "Waiting: " + waitingFor;
        return statusText(st);
    }

    private static String statusStyle(String st) {
        String base = "border-radius:999px; padding:2px 10px; font-size:12px; font-weight:600; ";
        if ("CO".equals(st) || "CL".equals(st))
            return base + "background:#dcfce7; color:#166534;";
        if ("VO".equals(st) || "RE".equals(st) || "NA".equals(st))
            return base + "background:#fee2e2; color:#991b1b;";
        return base + "background:#dbeafe; color:#1d4ed8;";
    }

    // ================================================================== UI

    private void styleInputs() {
        for (Listbox lb : new Listbox[] { orgList, whList, salesTypeList,
                territoryList, customerList, categoryList, productList,
                priceListList, paymentTermList, locationList, departmentList }) {
            lb.setMold("select");
            lb.setStyle(INPUT);
            lb.addEventListener(Events.ON_SELECT, this);
        }
        whList.setDisabled(true);                       // Depot comes from the Territory
        payCash.setValue("C");
        payCredit.setValue("R");
        paySample.setValue("S");
        payCash.setStyle("margin-right:14px;");
        payCredit.setStyle("margin-right:14px;");
        payGroup.appendChild(payCash);
        payGroup.appendChild(payCredit);
        payGroup.appendChild(paySample);
        payGroup.setSelectedItem(payCash);
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
        btn(backBtn, "#ffffff", "#334155", "#cbd5e1");
        btn(createBtn, "#2563eb", "#ffffff", null);
        btn(searchBtn, "#2563eb", "#ffffff", null);
        btn(resetBtn, "#ffffff", "#334155", "#cbd5e1");

        // order list filters
        fFrom.setFormat("dd-MMM-yyyy");
        fTo.setFormat("dd-MMM-yyyy");
        ZKUpdateUtil.setWidth(fFrom, "100%");
        ZKUpdateUtil.setWidth(fTo, "100%");
        fCustomer.setStyle(INPUT);
        fCustomer.setPlaceholder("Part of the customer name");
        fStatus.setMold("select");
        fStatus.setStyle(INPUT);
        fStatus.appendItem("All", "");
        fStatus.appendItem("Draft", "DR");
        fStatus.appendItem("In Progress", "IP");
        fStatus.appendItem("Completed", "CO");
        fStatus.appendItem("Closed", "CL");
        fStatus.appendItem("Voided", "VO");
        resetOrderFilters();

        segmentLbl.setStyle("font-weight:700; color:#1d4ed8; background:#dbeafe; padding:3px 14px; "
                          + "border-radius:999px;");
        officerLbl.setStyle("font-weight:700; color:#0f172a;");
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
        Label title = titleLbl;
        title.setStyle("display:block; font-size:22px; font-weight:700; color:#0f172a;");
        Label sub = new Label("Segment, Sales Type and Territory come from your Officer Assignment");
        sub.setStyle("display:block; color:#64748b; font-size:13px; margin-top:2px;");
        left.appendChild(title);
        left.appendChild(sub);

        Div right = new Div();
        right.setStyle("display:flex; align-items:center; flex-wrap:wrap;");
        right.appendChild(backBtn);
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
        Div segCell = new Div();
        segCell.appendChild(segmentLbl);
        addRow(body, "Segment", segCell, "Officer", officerLbl);
        addRow(body, "Sales Type *", salesTypeList, "Territory *", territoryList);
        addRow(body, "Customer *", customerList, "Depot (from Territory)", whList);
        addRow(body, "Organization *", orgList, "Price List *", priceListList);
        addRow(body, "Order Date *", orderDate, "Expected Delivery", deliveryDate);
        addRow(body, "Location *", locationList, "Department *", departmentList);
        addRow(body, "Payment Type *", payGroup, "Payment Term", paymentTermList);
        addRow(body, "Document No", docNoLbl, "", new Label(""));
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
    }

    /** Depot is not chosen by the user: it is the Warehouse of the selected Territory. */
    private void loadWarehouses() {
        Territory t = selectedTerritory();
        List<Option> one = new ArrayList<Option>();
        if (t != null && t.warehouseId() > 0)
            one.add(new Option(t.warehouseId(), t.warehouseName()));
        fill(whList, one, Option::getId, Option::getName);
        if (whList.getItemCount() == 2)
            whList.setSelectedIndex(1);
        whList.setDisabled(true);
        refreshStock();
    }

    private void loadHeaderLookups() {
        fill(priceListList, salesOrderService.getPriceLists(), Option::getId, Option::getName);
        if (priceListList.getItemCount() == 2)
            priceListList.setSelectedIndex(1);
        fill(locationList, headerLookupService.getLocations(), Option::getId, Option::getName);
        fill(departmentList, headerLookupService.getDepartments(), Option::getId, Option::getName);
        payGroup.setSelectedItem(payCash);
        fill(paymentTermList, salesOrderService.getPaymentTerms(), Option::getId, Option::getName);
    }

    /** Segment, Officer, Sales Types and Territories: all from the officer's assignment. */
    private void applyScope() {
        if (scope == null) {
            segmentLbl.setValue("-");
            officerLbl.setValue("");
            fill(salesTypeList, new ArrayList<IdName>(), IdName::id, IdName::name);
            loadTerritories();
            return;
        }
        segmentLbl.setValue(scope.segmentName());
        officerLbl.setValue(scope.officerName());
        fill(salesTypeList, scope.salesTypes(), IdName::id, IdName::name);
        if (salesTypeList.getItemCount() == 2)
            salesTypeList.setSelectedIndex(1);          // one Sales Type only: choose it for the user
        loadTerritories();
        loadCategories();
    }

    /** Territories of the selected Sales Type (the officer's own regions only). */
    private void loadTerritories() {
        List<Territory> list = scope == null ? new ArrayList<Territory>()
                : scope.territoriesFor(selectedId(salesTypeList));
        fill(territoryList, list, Territory::regionId, Territory::name);
        if (territoryList.getItemCount() == 2)
            territoryList.setSelectedIndex(1);          // one Territory only: choose it for the user
        loadCustomers();
    }

    /** Customers of the selected Territory. */
    private void loadCustomers() {
        loadWarehouses();
        fill(customerList, customerService.getCustomers(selectedId(territoryList)),
                Customer::getId, Customer::getName);
        lines.clear();
        refreshLines();
        clearCustomerInfo();
    }

    private Territory selectedTerritory() {
        if (scope == null)
            return null;
        int regionId = selectedId(territoryList);
        int docTypeId = selectedId(salesTypeList);
        for (Territory t : scope.territories()) {
            if (t.regionId() == regionId && t.docTypeId() == docTypeId)
                return t;
        }
        return null;
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
    }

    private void clearCustomerInfo() {
        custNameLbl.setValue("");
        binLbl.setValue("");
        vatRateLbl.setValue("");
        creditLimitLbl.setValue("");
        paymentTermLbl.setValue("");
        creditDaysLbl.setValue("");
        contactLbl.setValue("");
        addressLbl.setValue("");
        if (categoryList.getItemCount() > 0)
            categoryList.setSelectedIndex(0);
        clearList(productList);
        clearProductInfo();
        qtyBox.setValue((BigDecimal) null);
        spDiscBox.setValue((BigDecimal) null);
    }

    // ---- Category -> Product (the Segment is fixed: it is the officer's Segment)

    private void loadCategories() {
        List<Category> cats = scope == null ? new ArrayList<Category>()
                : categoryService.getSegmentCategories(scope.segmentId());
        fill(categoryList, cats, Category::getId, Category::getName);
        segWarnLbl.setValue(scope != null && cats.isEmpty()
                ? "No product category is linked to your Segment (" + scope.segmentName() + ")." : "");
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
                    scope == null ? 0 : scope.segmentId(), scope == null ? "" : scope.segmentName(),
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
        if (categoryList.getItemCount() > 0)
            categoryList.setSelectedIndex(0);
        loadProducts();
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
            e.setDisabled(!editable);
            e.addEventListener(Events.ON_CLICK, this);
            Button r = new Button("Remove");
            r.setAttribute("act", "remove");
            r.setAttribute("idx", Integer.valueOf(n - 1));
            r.setDisabled(!editable);
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
            if (t instanceof Listitem && ((Component) t).getAttribute("orderId") != null) {
                openOrder(((Integer) ((Component) t).getAttribute("orderId")).intValue());
                return;
            }
        }

        if (t == orgList) {
            // nothing to do: the Depot follows the Territory
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
            boolean wasStored = editingOrderId > 0;
            resetForm();
            if (wasStored) {
                showList();
                refreshOrderList();
            }
        } else if (t == saveBtn) {
            saveDraft();
        } else if (t == createBtn) {
            openCreate();
        } else if (t == backBtn) {
            showList();
            refreshOrderList();
        } else if (t == searchBtn) {
            refreshOrderList();
        } else if (t == resetBtn) {
            resetOrderFilters();
            refreshOrderList();
        } else if (t == completeBtn) {
            submitOrder();
        } else if (t == printBtn) {
            Messagebox.show("Later",
                    "Coming next", Messagebox.OK, Messagebox.INFORMATION);
        }
    }

    // ================================================================ save

    private OrderHeader readHeader() {
        OrderHeader h = new OrderHeader();
        h.setOrgId(selectedId(orgList));
        h.setWarehouseId(selectedId(whList));
        h.setDocTypeId(selectedId(salesTypeList));
        h.setBpartnerId(selectedId(customerList));
        h.setPriceListId(selectedId(priceListList));
        h.setSalesRepId(scope == null ? 0 : scope.officerId());
        h.setPaymentTermId(selectedId(paymentTermList));
        h.setDateOrdered(toTimestamp(orderDate.getValue()));
        h.setDatePromised(toTimestamp(deliveryDate.getValue()));
        h.setDescription(notesBox.getValue());
        h.setSegmentId(scope == null ? 0 : scope.segmentId());
        h.setTerritoryId(selectedId(territoryList));
        h.setSiteDelivery(siteDelivery.isChecked());
        h.setSiteAddress(siteAddress.getValue());
        h.setLocationId(selectedId(locationList));
        h.setDepartmentId(selectedId(departmentList));
        h.setPaymentType(payGroup.getSelectedItem() == null ? ""
                : (String) payGroup.getSelectedItem().getValue());
        return h;
    }

    private java.sql.Timestamp toTimestamp(java.util.Date d) {
        return d == null ? null : new java.sql.Timestamp(d.getTime());
    }

    /**
     * Stores the screen as a Draft (new order) or updates the stored Draft. One click = one transaction.
     * Remembers the order id, so the next save updates the same order.
     */
    private SavedOrder persistDraft() {
        OrderHeader h = readHeader();
        SavedOrder so = editingOrderId > 0
                ? salesOrderService.updateDraft(editingOrderId, h, lines)
                : salesOrderService.saveDraft(h, lines);
        editingOrderId = so.getOrderId();
        docNoLbl.setValue(so.getDocumentNo());
        titleLbl.setValue("Sales Order " + so.getDocumentNo());
        statusBadge.setValue("Draft: " + so.getDocumentNo());
        return so;
    }

    private boolean canWork() {
        if (!editable) {
            warn("This order is not a Draft any more, so it cannot be changed.");
            return false;
        }
        if (scope == null) {
            warn(scopeError == null ? "You have no current Officer Assignment." : scopeError);
            return false;
        }
        return !saving;
    }

    private void saveDraft() {
        if (!canWork())
            return;
        saving = true;
        saveBtn.setDisabled(true);
        completeBtn.setDisabled(true);
        try {
            SavedOrder so = persistDraft();
            Messagebox.show("Draft saved.\nDocument No: " + so.getDocumentNo()
                    + "\nStatus: " + so.getDocStatus()
                    + "\nLines total: " + TAKA + fmt(so.getTotalLines())
                    + "\nGrand total (iDempiere): " + TAKA + fmt(so.getGrandTotal()),
                    "Sales Order", Messagebox.OK, Messagebox.INFORMATION);
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (RuntimeException ex) {
            warn("Could not save the order. Nothing was stored.\n" + ex.getMessage());
        } finally {
            saving = false;
            saveBtn.setDisabled(!editable);
            completeBtn.setDisabled(!editable);
        }
    }

    /**
     * Submit for Approval: checks the Zone Manager exists, stores the Draft, then starts the
     * approval workflow. The approval layers are configured in iDempiere (docs/APPROVAL_SETUP.md).
     */
    private void submitOrder() {
        if (!canWork())
            return;
        saving = true;
        saveBtn.setDisabled(true);
        completeBtn.setDisabled(true);
        try {
            ZoneManager zm = salesOrderService.requireApprover(readHeader());   // fail before anything is stored
            persistDraft();
            SubmitResult r = salesOrderService.submit(editingOrderId, headerLookupService.getCurrentUserId());
            statusBadge.setValue(statusFor(r.docStatus(), r.waitingFor()));
            setEditable(false);
            Messagebox.show("Order " + r.documentNo() + " was submitted for approval.\n"
                    + "Zone Manager: " + zm.userName() + " (" + zm.zoneName() + ")\n"
                    + "Status: " + statusFor(r.docStatus(), r.waitingFor()),
                    "Sales Order", Messagebox.OK, Messagebox.INFORMATION);
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (RuntimeException ex) {
            warn("Could not submit the order.\n" + ex.getMessage());
        } finally {
            saving = false;
            saveBtn.setDisabled(!editable);
            completeBtn.setDisabled(!editable);
        }
    }

    /** Opens an order of the officer from the list: a Draft can be edited, any other status is read only. */
    private void openOrder(int orderId) {
        if (scope == null) {
            warn(scopeError == null ? "You have no current Officer Assignment." : scopeError);
            return;
        }
        OrderDetail d;
        try {
            d = salesOrderService.loadOrder(orderId, scope.officerId());
        } catch (RuntimeException ex) {
            warn("Could not open the order.\n" + ex.getMessage());
            return;
        }
        if (d == null) {
            warn("The order was not found.");
            refreshOrderList();
            return;
        }
        resetForm();
        OrderHeader h = d.header();
        selectById(salesTypeList, h.getDocTypeId());
        loadTerritories();
        selectById(territoryList, h.getTerritoryId());
        loadCustomers();                                   // also clears the lines: they are set below
        selectById(customerList, h.getBpartnerId());
        loadCustomerInfo(h.getBpartnerId());
        selectById(whList, h.getWarehouseId());
        selectById(orgList, h.getOrgId());
        selectById(priceListList, h.getPriceListId());
        selectById(paymentTermList, h.getPaymentTermId());
        selectById(locationList, h.getLocationId());
        selectById(departmentList, h.getDepartmentId());
        orderDate.setValue(h.getDateOrdered());
        deliveryDate.setValue(h.getDatePromised());
        notesBox.setValue(h.getDescription());
        for (Radio rb : new Radio[] { payCash, payCredit, paySample })
            if (h.getPaymentType().equals(rb.getValue()))
                payGroup.setSelectedItem(rb);
        siteDelivery.setChecked(h.isSiteDelivery());
        applySiteDelivery(h.isSiteDelivery());
        if (h.isSiteDelivery())
            siteAddress.setValue(h.getSiteAddress());

        lines.clear();
        lines.addAll(d.lines());
        editingOrderId = d.orderId();
        docNoLbl.setValue(d.documentNo());
        titleLbl.setValue("Sales Order " + d.documentNo());
        statusBadge.setValue(d.returnedInfo().isEmpty() ? statusFor(d.docStatus(), d.waitingFor())
                : "Draft (returned)");
        setEditable("DR".equals(d.docStatus()));           // also redraws the lines
        showForm();
        if (!d.returnedInfo().isEmpty())
            Messagebox.show(d.returnedInfo() + "\n\nChange the order and submit it for approval again.",
                    "Order returned", Messagebox.OK, Messagebox.EXCLAMATION);
    }

    /** Draft: everything can be changed. Any other status: the screen is read only. */
    private void setEditable(boolean on) {
        editable = on;
        saveBtn.setDisabled(!on);
        completeBtn.setDisabled(!on);
        addBtn.setDisabled(!on);
        clearBtn.setDisabled(!on);
        Component[] inputs = { orgList, whList, salesTypeList, territoryList, customerList, orderDate,
                deliveryDate, priceListList, locationList, departmentList, paymentTermList, notesBox,
                siteDelivery, siteAddress, categoryList, productList, qtyBox, spDiscBox,
                payCash, payCredit, paySample };
        for (Component c : inputs)
            setDisabled(c, !on);
        refreshLines();
    }

    private static void setDisabled(Component c, boolean disabled) {
        if (c instanceof Listbox)
            ((Listbox) c).setDisabled(disabled);
        else if (c instanceof Checkbox)                    // Radio is a Checkbox
            ((Checkbox) c).setDisabled(disabled);
        else if (c instanceof org.zkoss.zul.impl.InputElement)
            ((org.zkoss.zul.impl.InputElement) c).setDisabled(disabled);
    }

    private boolean lockedAfterSave() {
        if (editable)
            return false;
        warn("This order is not a Draft any more, so it cannot be changed.");
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
        editingOrderId = 0;
        titleLbl.setValue("New Sales Order");
        cancelBtn.setLabel("Cancel");
        statusBadge.setValue("Draft (not saved)");
        docNoLbl.setValue("Assigned on save");
        notesBox.setValue("");
        orderDate.setValue(new java.util.Date());
        deliveryDate.setValue(null);
        siteDelivery.setChecked(false);
        applySiteDelivery(false);
        loadOrgs();
        loadHeaderLookups();
        applyScope();
        setEditable(true);
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
