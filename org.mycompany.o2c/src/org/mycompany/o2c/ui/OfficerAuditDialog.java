package org.mycompany.o2c.ui;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.mycompany.o2c.model.AuditRows.Approval;
import org.mycompany.o2c.model.AuditRows.Change;
import org.mycompany.o2c.model.AuditRows.Order;
import org.mycompany.o2c.model.AuditRows.Session;
import org.mycompany.o2c.model.OfficerAssignment;
import org.mycompany.o2c.service.AuditService;
import org.mycompany.o2c.util.ValidationException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Window;

/**
 * Officer Audit (read only): what the officer of an assignment did in a period: sales orders (created,
 * changed after creation, status), the Change Log of an order, approval steps and login sessions.
 * Nothing is changed here. UI only: queries are in AuditService / AuditDAO.
 */
public class OfficerAuditDialog {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private final AuditService service = new AuditService();
    private final OfficerAssignment a;
    private final Window win = new Window();
    private final Datebox from = new Datebox();
    private final Datebox to = new Datebox();
    private final Label summary = new Label("");
    private final Label msg = new Label("");
    private final Label changeHint = new Label("Select an order in the Orders tab.");
    private final Listbox orders = list("Order No:10%", "Order Date:9%", "Customer:18%", "Region:12%",
            "Segment:9%", "Status:8%", "Total:9%", "Created:11%", "Last updated:11%", "Updated by:10%");
    private final Listbox changes = list("When:16%", "Who:16%", "Record:8%", "Column:20%", "Old value:20%",
            "New value:20%");
    private final Listbox approvals = list("When:14%", "Order No:12%", "Step:16%", "State:8%",
            "Assigned to:16%", "Done by:16%", "Message:18%");
    private final Listbox sessions = list("Login:20%", "Last activity:20%", "IP:20%", "Role:30%", "Closed:10%");
    private Tabbox tabs;
    private Tab changeTab;

    public OfficerAuditDialog(OfficerAssignment a) {
        this.a = a;
    }

    public void open(Component parent) {
        win.setTitle("Audit: " + a.officerName());
        win.setBorder("normal");
        win.setClosable(true);
        win.setWidth("1100px");
        win.setPosition("center");
        win.setParent(parent);

        Div body = new Div();
        body.setStyle("padding:12px 16px;");

        Label who = new Label(a.officerName() + "   |   " + a.docTypeName() + " / " + a.activityName()
                + "   |   " + a.regionName());
        who.setStyle("display:block; font-weight:600; color:#0f172a;");
        Label period = new Label("Assignment: " + day(a.startDate()) + " to "
                + (a.endDate() == null ? "today (still assigned)" : day(a.endDate())));
        period.setStyle("display:block; font-size:13px; color:#64748b; margin:2px 0 10px 0;");
        body.appendChild(who);
        body.appendChild(period);

        Hlayout bar = new Hlayout();
        bar.setStyle("align-items:center;");
        from.setFormat("dd-MM-yyyy");
        to.setFormat("dd-MM-yyyy");
        from.setValueInLocalDate(a.startDate().toLocalDateTime().toLocalDate());
        to.setValueInLocalDate(a.endDate() == null ? LocalDate.now()
                : a.endDate().toLocalDateTime().toLocalDate());
        Button go = new Button("Show");
        go.setStyle("background:#2563eb; color:#ffffff; border:none; border-radius:8px; padding:6px 16px;");
        go.addEventListener(Events.ON_CLICK, e -> load());
        bar.appendChild(new Label("From"));
        bar.appendChild(from);
        bar.appendChild(new Label("To"));
        bar.appendChild(to);
        bar.appendChild(go);
        body.appendChild(bar);

        summary.setStyle("display:block; margin:10px 0; font-size:13px; color:#0f172a;");
        msg.setStyle("display:block; color:#b91c1c; font-size:13px;");
        body.appendChild(summary);
        body.appendChild(msg);

        tabs = new Tabbox();
        ZKUpdateUtil.setWidth(tabs, "100%");
        Tabs heads = new Tabs();
        Tab ordersTab = new Tab("Orders");
        changeTab = new Tab("Change Log");
        heads.appendChild(ordersTab);
        heads.appendChild(changeTab);
        heads.appendChild(new Tab("Approvals"));
        heads.appendChild(new Tab("Sessions"));
        tabs.appendChild(heads);
        Tabpanels panels = new Tabpanels();
        panels.appendChild(panel(orders));
        Tabpanel changePanel = panel(changes);
        changeHint.setStyle("display:block; font-size:12px; color:#64748b; margin-bottom:6px;");
        changePanel.insertBefore(changeHint, changePanel.getFirstChild());
        panels.appendChild(changePanel);
        panels.appendChild(panel(approvals));
        panels.appendChild(panel(sessions));
        tabs.appendChild(panels);
        body.appendChild(tabs);

        win.appendChild(body);
        orders.addEventListener(Events.ON_SELECT, e -> loadChanges());
        load();
        win.doModal();
    }

    private void load() {
        msg.setValue("");
        LocalDate f = from.getValueInLocalDate();
        LocalDate t = to.getValueInLocalDate();
        try {
            List<Order> o = service.orders(a.officerId(), 0, f, t);
            clear(orders);
            clear(changes);
            changeHint.setValue("Select an order in the Orders tab.");
            int done = 0, draft = 0, inApproval = 0, other = 0, edited = 0;
            BigDecimal total = BigDecimal.ZERO;
            for (Order r : o) {
                orders.appendChild(orderItem(r));
                if ("CO".equals(r.docStatus()) || "CL".equals(r.docStatus()))
                    done++;
                else if ("DR".equals(r.docStatus()))
                    draft++;
                else if ("IP".equals(r.docStatus()))
                    inApproval++;
                else
                    other++;
                if (r.edited())
                    edited++;
                if (r.grandTotal() != null)
                    total = total.add(r.grandTotal());
            }
            summary.setValue(o.size() + " orders  (completed " + done + ", draft " + draft + ", in approval "
                    + inApproval + ", void/rejected/other " + other + "),  changed after creation: " + edited
                    + ",  total " + MONEY.format(total));

            clear(approvals);
            for (Approval r : service.approvals(a.officerId(), f, t))
                approvals.appendChild(item(time(r.created()), r.orderNo(), r.step(), r.state(), r.assignedTo(),
                        r.doneBy(), r.message()));
            clear(sessions);
            for (Session r : service.sessions(a.officerId(), f, t))
                sessions.appendChild(item(time(r.login()), time(r.lastSeen()), r.ip(), r.role(),
                        r.closed() ? "Yes" : "No"));
        } catch (ValidationException ex) {
            msg.setValue(ex.getMessage());
        } catch (RuntimeException ex) {
            Messagebox.show("Could not load the audit: " + ex.getMessage(), "Audit", Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    private Listitem orderItem(Order r) {
        Listitem it = item(r.documentNo(), day(r.dateOrdered()), r.customer(), r.region(), r.segment(),
                r.docStatus(), r.grandTotal() == null ? "" : MONEY.format(r.grandTotal()),
                time(r.created()) + " " + r.createdBy(),
                r.edited() ? time(r.updated()) : "-", r.edited() ? r.updatedBy() : "-");
        it.setValue(r);
        return it;
    }

    private void loadChanges() {
        Listitem sel = orders.getSelectedItem();
        if (sel == null || !(sel.getValue() instanceof Order r))
            return;
        clear(changes);
        List<Change> list;
        try {
            list = service.changes(r.id());
        } catch (RuntimeException ex) {
            msg.setValue("Could not load the change log: " + ex.getMessage());
            return;
        }
        for (Change c : list)
            changes.appendChild(item(time(c.when()), c.who(), c.what(), c.column(), c.oldValue(), c.newValue()));
        changeHint.setValue("Order " + r.documentNo() + ": " + list.size() + " logged change(s). "
                + (list.isEmpty() ? "Only fields with Allow Logging (Application Dictionary) are logged, from the "
                + "day it was switched on." : ""));
        tabs.setSelectedTab(changeTab);
    }

    // ---------------------------------------------------------------- helpers

    private static Listbox list(String... heads) {
        Listbox l = new Listbox();
        ZKUpdateUtil.setWidth(l, "100%");
        ZKUpdateUtil.setHeight(l, "300px");
        l.setEmptyMessage("Nothing found.");
        Listhead h = new Listhead();
        for (String s : heads) {
            int i = s.lastIndexOf(':');
            Listheader lh = new Listheader(s.substring(0, i));
            lh.setWidth(s.substring(i + 1));
            h.appendChild(lh);
        }
        l.appendChild(h);
        return l;
    }

    private static Tabpanel panel(Component c) {
        Tabpanel p = new Tabpanel();
        p.appendChild(c);
        return p;
    }

    private static Listitem item(String... cells) {
        Listitem it = new Listitem();
        for (String c : cells)
            it.appendChild(new Listcell(c == null ? "" : c));
        return it;
    }

    private static void clear(Listbox box) {
        for (int i = box.getItemCount() - 1; i >= 0; i--)
            box.removeItemAt(i);
    }

    private static String day(Timestamp t) {
        return t == null ? "" : t.toLocalDateTime().toLocalDate().format(DAY);
    }

    private static String time(Timestamp t) {
        return t == null ? "" : t.toLocalDateTime().format(TIME);
    }
}
