package org.mycompany.o2c.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.mycompany.o2c.model.AssignmentFilter;
import org.mycompany.o2c.model.AssignmentFilter.Status;
import org.mycompany.o2c.model.IdName;
import org.mycompany.o2c.model.OfficerAssignment;
import org.mycompany.o2c.model.RegionRow;
import org.mycompany.o2c.service.OfficerAssignmentService;
import org.mycompany.o2c.util.ValidationException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Window;

/**
 * Officer Assignment form (UI layer only): list first, a Create button opens a popup.
 * No SQL and no business rule here: see OfficerAssignmentService / OfficerAssignmentDAO.
 */
public class WOfficerAssignmentForm extends ADForm {
    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final String CARD =
        "background:#ffffff; border:1px solid #e2e8f0; border-radius:12px; "
      + "box-shadow:0 1px 3px rgba(15,23,42,0.08); margin:12px; padding:16px 20px;";
    private static final String INPUT =
        "width:100%; padding:6px 8px; border:1px solid #cbd5e1; border-radius:8px; "
      + "background:#ffffff; box-sizing:border-box;";
    private static final String FIELD_LABEL = "display:block; font-size:12px; color:#64748b; margin-bottom:4px;";
    private static final String GRID =
        "display:grid; grid-template-columns:repeat(auto-fit, minmax(210px, 1fr)); gap:12px 16px;";
    private static final String PRIMARY =
        "background:#2563eb; color:#ffffff; border:none; border-radius:8px; padding:8px 18px; "
      + "font-weight:600; cursor:pointer;";
    private static final String SECONDARY =
        "background:#ffffff; color:#334155; border:1px solid #cbd5e1; border-radius:8px; "
      + "padding:8px 18px; cursor:pointer;";
    private static final String SMALL_BTN =
        "background:#ffffff; color:#b91c1c; border:1px solid #fca5a5; border-radius:6px; "
      + "padding:2px 12px; cursor:pointer;";
    private static final String BADGE_ON =
        "background:#dcfce7; color:#166534; border-radius:999px; padding:2px 10px; font-size:12px; font-weight:600;";
    private static final String BADGE_OFF =
        "background:#f1f5f9; color:#475569; border-radius:999px; padding:2px 10px; font-size:12px; font-weight:600;";
    private static final String ERROR = "color:#b91c1c; font-size:13px;";

    private final OfficerAssignmentService service = new OfficerAssignmentService();

    // ---- main list filters
    private final Listbox fType = select();
    private final Listbox fSegment = select();
    private final Listbox fLevel = select();
    private final Listbox fLocation = select();
    private final Combobox fOfficer = new Combobox();
    private final Radiogroup fStatus = new Radiogroup();
    private final Radio rActive = new Radio("Active");
    private final Radio rEnded = new Radio("Ended");
    private final Radio rAll = new Radio("All");

    // ---- main list
    private final Listbox grid = new Listbox();
    private final Label countLbl = new Label("");

    @Override
    protected void initForm() {
        setStyle("background:#f1f5f9;");
        ZKUpdateUtil.setWidth(this, "99%");
        ZKUpdateUtil.setHeight(this, "100%");

        Div root = new Div();
        root.setStyle("height:100%; overflow:auto;");
        appendChild(root);

        root.appendChild(buildHeader());
        root.appendChild(buildFilters());
        root.appendChild(buildListCard());

        try {
            loadFilterData();
        } catch (RuntimeException ex) {
            Messagebox.show("Could not load the form data: " + ex.getMessage(), "Officer Assignment",
                    Messagebox.OK, Messagebox.ERROR);
        }
        refresh();
    }

    // =============================================================== main screen

    private Component buildHeader() {
        Div bar = new Div();
        bar.setStyle("display:flex; align-items:center; justify-content:space-between; margin:12px 12px 0 12px;");

        Div titles = new Div();
        Label title = new Label("Officer Assignment");
        title.setStyle("display:block; font-size:22px; font-weight:700; color:#0f172a;");
        Label sub = new Label("Who looks after which region. An officer is replaced by giving the old one an End Date.");
        sub.setStyle("display:block; font-size:13px; color:#64748b; margin-top:2px;");
        titles.appendChild(title);
        titles.appendChild(sub);

        Button create = new Button("+ Create Assignment");
        create.setStyle(PRIMARY);
        create.addEventListener(Events.ON_CLICK, e -> new CreateDialog().open());

        bar.appendChild(titles);
        bar.appendChild(create);
        return bar;
    }

    private Component buildFilters() {
        Div card = new Div();
        card.setStyle(CARD);

        Div g = new Div();
        g.setStyle(GRID);

        ZKUpdateUtil.setWidth(fOfficer, "100%");
        fOfficer.setAutodrop(true);
        fOfficer.setAutocomplete(true);
        fOfficer.setPlaceholder("All officers");

        fStatus.appendChild(rActive);
        fStatus.appendChild(rEnded);
        fStatus.appendChild(rAll);
        fStatus.setSelectedItem(rActive);
        Div statusBox = new Div();
        statusBox.setStyle("padding-top:6px;");
        statusBox.appendChild(fStatus);

        g.appendChild(field("Sales Type", fType));
        g.appendChild(field("Segment", fSegment));
        g.appendChild(field("Level", fLevel));
        g.appendChild(field("Location (parent region)", fLocation));
        g.appendChild(field("Officer", fOfficer));
        g.appendChild(field("Status", statusBox));
        card.appendChild(g);

        Hlayout buttons = new Hlayout();
        buttons.setStyle("margin-top:14px;");
        Button search = new Button("Search");
        search.setStyle(PRIMARY);
        search.addEventListener(Events.ON_CLICK, e -> refresh());
        Button reset = new Button("Reset");
        reset.setStyle(SECONDARY);
        reset.addEventListener(Events.ON_CLICK, e -> resetFilters());
        buttons.appendChild(search);
        buttons.appendChild(reset);
        card.appendChild(buttons);

        fSegment.addEventListener(Events.ON_SELECT, e -> onFilterSegmentChange());
        fLevel.addEventListener(Events.ON_SELECT,
                e -> reloadLocations(fLevel, fLocation, idOf(fSegment), "All"));
        return card;
    }

    private Component buildListCard() {
        Div card = new Div();
        card.setStyle(CARD);

        countLbl.setStyle("display:block; font-size:13px; color:#64748b; margin-bottom:8px;");
        card.appendChild(countLbl);

        grid.setMold("paging");
        grid.setPageSize(15);
        grid.setEmptyMessage("No assignments found.");
        ZKUpdateUtil.setWidth(grid, "100%");

        Listhead head = new Listhead();
        head.appendChild(header("Region", "11%"));
        head.appendChild(header("Path", "22%"));
        head.appendChild(header("Sales Type", "11%"));
        head.appendChild(header("Segment", "11%"));
        head.appendChild(header("Officer", "15%"));
        head.appendChild(header("Start Date", "8%"));
        head.appendChild(header("End Date", "8%"));
        head.appendChild(header("Status", "7%"));
        head.appendChild(header("", "13%"));
        grid.appendChild(head);

        card.appendChild(grid);
        return card;
    }

    private void loadFilterData() {
        fill(fType, service.salesTypes(), "All");
        fill(fSegment, service.segments(), "All");
        onFilterSegmentChange();
    }

    /** Level, Location and Officer of the filter follow the chosen Segment. */
    private void onFilterSegmentChange() {
        int seg = idOf(fSegment);
        fill(fLevel, service.levels(seg), "All");
        reloadLocations(fLevel, fLocation, seg, "All");
        fillCombo(fOfficer, service.historyOfficers(seg));
    }

    private void resetFilters() {
        fType.setSelectedIndex(0);
        fSegment.setSelectedIndex(0);
        onFilterSegmentChange();
        fOfficer.setValue("");
        fStatus.setSelectedItem(rActive);
        refresh();
    }

    private void refresh() {
        Status st = fStatus.getSelectedItem() == rEnded ? Status.ENDED
                : fStatus.getSelectedItem() == rAll ? Status.ALL : Status.ACTIVE;
        AssignmentFilter f = new AssignmentFilter(idOf(fType), idOf(fSegment), idOf(fOfficer),
                idOf(fLevel), idOf(fLocation), st);
        List<OfficerAssignment> rows;
        try {
            rows = service.search(f);
        } catch (RuntimeException ex) {
            Messagebox.show("Could not load the list: " + ex.getMessage(), "Officer Assignment",
                    Messagebox.OK, Messagebox.ERROR);
            return;
        }
        clearItems(grid);
        for (OfficerAssignment a : rows)
            grid.appendChild(toItem(a));
        countLbl.setValue(rows.size() + (rows.size() == 1 ? " assignment" : " assignments"));
    }

    private Listitem toItem(OfficerAssignment a) {
        Listitem it = new Listitem();
        Listcell region = new Listcell(a.regionName());
        region.setStyle("font-weight:600;");
        it.appendChild(region);
        it.appendChild(new Listcell(service.path(a.regionId())));
        it.appendChild(new Listcell(a.docTypeName()));
        it.appendChild(new Listcell(a.activityName()));
        it.appendChild(new Listcell(a.officerName()));
        it.appendChild(new Listcell(a.startDate().toLocalDateTime().toLocalDate().format(FMT)));
        it.appendChild(new Listcell(a.endDate() == null ? "-"
                : a.endDate().toLocalDateTime().toLocalDate().format(FMT)));

        Listcell status = new Listcell();
        Label badge = new Label(a.isOpen() ? "Active" : "Ended");
        badge.setStyle(a.isOpen() ? BADGE_ON : BADGE_OFF);
        status.appendChild(badge);
        it.appendChild(status);

        Listcell action = new Listcell();
        Button audit = new Button("Audit");
        audit.setStyle(SMALL_BTN.replace("#b91c1c", "#1d4ed8").replace("#fca5a5", "#93c5fd")
                + " margin-right:4px;");
        audit.addEventListener(Events.ON_CLICK, e -> new OfficerAuditDialog(a).open(this));
        action.appendChild(audit);
        if (a.isOpen()) {
            Button end = new Button("End");
            end.setStyle(SMALL_BTN);
            end.addEventListener(Events.ON_CLICK, e -> openEndDialog(a));
            action.appendChild(end);
        }
        it.appendChild(action);
        return it;
    }

    // =============================================================== End dialog

    private void openEndDialog(OfficerAssignment a) {
        Window win = new Window();
        win.setTitle("End Assignment");
        win.setBorder("normal");
        win.setClosable(true);
        win.setWidth("440px");
        win.setPosition("center");
        win.setParent(this);

        Div body = new Div();
        body.setStyle("padding:14px 16px;");

        Label who = new Label(a.officerName() + "  →  " + a.regionName());
        who.setStyle("display:block; font-weight:600; color:#0f172a;");
        Label what = new Label(a.docTypeName() + " / " + a.activityName() + ", since "
                + a.startDate().toLocalDateTime().toLocalDate().format(FMT));
        what.setStyle("display:block; font-size:13px; color:#64748b; margin:2px 0 12px 0;");

        Datebox endDate = new Datebox();
        endDate.setFormat("dd-MM-yyyy");
        endDate.setValueInLocalDate(LocalDate.now());
        ZKUpdateUtil.setWidth(endDate, "100%");

        Label msg = new Label("");
        msg.setStyle(ERROR + " display:block; margin-top:8px;");

        Hlayout buttons = new Hlayout();
        buttons.setStyle("margin-top:14px;");
        Button cancel = new Button("Cancel");
        cancel.setStyle(SECONDARY);
        cancel.addEventListener(Events.ON_CLICK, e -> win.detach());
        Button ok = new Button("End Assignment");
        ok.setStyle(PRIMARY);
        ok.addEventListener(Events.ON_CLICK, e -> {
            try {
                service.end(a.id(), endDate.getValueInLocalDate());
                win.detach();
                Clients.showNotification("Assignment ended", "info", null, "top_center", 3000);
                refresh();
            } catch (ValidationException ex) {
                msg.setValue(ex.getMessage());
            } catch (RuntimeException ex) {
                msg.setValue("Could not save: " + ex.getMessage());
            }
        });
        buttons.appendChild(cancel);
        buttons.appendChild(ok);

        body.appendChild(who);
        body.appendChild(what);
        body.appendChild(field("End Date", endDate));
        body.appendChild(msg);
        body.appendChild(buttons);
        win.appendChild(body);
        win.doModal();
    }

    // =============================================================== Create dialog

    /** Popup: 1) where  2) which regions  3) who and from when. */
    private class CreateDialog {
        private final Window win = new Window();
        private final Listbox type = select();
        private final Listbox segment = select();
        private final Listbox level = select();
        private final Listbox location = select();
        private final Combobox officer = new Combobox();
        private final Datebox start = new Datebox();
        private final Checkbox replace = new Checkbox(
                "Replace current officer (the old officer's End Date becomes the day before Start Date)");
        private final Listbox regionList = new Listbox();
        private final Label hint = new Label("");
        private final Label msg = new Label("");
        private final Button assignBtn = new Button("Assign");

        void open() {
            win.setTitle("Create Assignment");
            win.setBorder("normal");
            win.setClosable(true);
            win.setWidth("780px");
            win.setPosition("center");
            win.setParent(WOfficerAssignmentForm.this);

            Div body = new Div();
            body.setStyle("padding:14px 18px;");

            // 1. where
            body.appendChild(step("1.  Sales Type, Segment and Level"));
            Div g1 = new Div();
            g1.setStyle(GRID);
            g1.appendChild(field("Sales Type", type));
            g1.appendChild(field("Segment", segment));
            g1.appendChild(field("Level", level));
            g1.appendChild(field("Location (parent region)", location));
            body.appendChild(g1);

            // 2. regions
            body.appendChild(step("2.  Regions"));
            hint.setStyle("display:block; font-size:12px; color:#64748b; margin-bottom:6px;");
            body.appendChild(hint);
            regionList.setCheckmark(true);
            regionList.setMultiple(true);
            ZKUpdateUtil.setWidth(regionList, "100%");
            ZKUpdateUtil.setHeight(regionList, "230px");
            regionList.setEmptyMessage("No regions.");
            Listhead head = new Listhead();
            head.appendChild(header("Region", "22%"));
            head.appendChild(header("Path", "45%"));
            head.appendChild(header("Current officer", "33%"));
            regionList.appendChild(head);
            body.appendChild(regionList);

            // 3. who
            body.appendChild(step("3.  Officer and Start Date"));
            Div g3 = new Div();
            g3.setStyle(GRID);
            ZKUpdateUtil.setWidth(officer, "100%");
            officer.setAutodrop(true);
            officer.setAutocomplete(true);
            officer.setPlaceholder("Type to search");
            start.setFormat("dd-MM-yyyy");
            start.setValueInLocalDate(LocalDate.now());
            ZKUpdateUtil.setWidth(start, "100%");
            g3.appendChild(field("Officer", officer));
            g3.appendChild(field("Start Date", start));
            body.appendChild(g3);
            replace.setStyle("display:block; margin-top:10px; font-size:13px;");
            body.appendChild(replace);

            msg.setStyle(ERROR + " display:block; margin-top:10px; min-height:18px;");
            body.appendChild(msg);

            Hlayout buttons = new Hlayout();
            buttons.setStyle("margin-top:12px; float:right;");
            Button cancel = new Button("Cancel");
            cancel.setStyle(SECONDARY);
            cancel.addEventListener(Events.ON_CLICK, e -> win.detach());
            assignBtn.setStyle(PRIMARY);
            assignBtn.addEventListener(Events.ON_CLICK, e -> onAssign());
            buttons.appendChild(cancel);
            buttons.appendChild(assignBtn);
            body.appendChild(buttons);
            Div clear = new Div();
            clear.setStyle("clear:both;");
            body.appendChild(clear);

            win.appendChild(body);

            // data
            try {
                fill(type, service.salesTypes(), "Select...");
                fill(segment, service.segments(), "Select...");
                fill(level, service.levels(0), "Select...");
                reloadLocations(level, location, 0, "All in this level");
                reloadOfficers();
            } catch (RuntimeException ex) {
                msg.setValue("Could not load data: " + ex.getMessage());
            }

            type.addEventListener(Events.ON_SELECT, e -> {
                reloadOfficers();
                renderRegions();
            });
            segment.addEventListener(Events.ON_SELECT, e -> {
                fill(level, service.levels(idOf(segment)), "Select...");
                reloadLocations(level, location, idOf(segment), "All in this level");
                reloadOfficers();
                renderRegions();
            });
            level.addEventListener(Events.ON_SELECT, e -> {
                reloadLocations(level, location, idOf(segment), "All in this level");
                renderRegions();
            });
            location.addEventListener(Events.ON_SELECT, e -> renderRegions());
            replace.addEventListener(Events.ON_CHECK, e -> renderRegions());
            regionList.addEventListener(Events.ON_SELECT, e -> updateAssignButton());

            renderRegions();
            win.doModal();
        }

        /** Only officers with Segment Access (and Document Access of the Sales Type) can be chosen. */
        private void reloadOfficers() {
            int s = idOf(segment);
            fillCombo(officer, s == 0 ? new ArrayList<IdName>() : service.officers(s, idOf(type)));
            officer.setPlaceholder(s == 0 ? "Select the Segment first" : "Type to search");
        }

        private void renderRegions() {
            msg.setValue("");
            Set<Integer> keep = selectedRegionIds();
            int t = idOf(type);
            int s = idOf(segment);
            int l = idOf(level);
            clearItems(regionList);
            if (t == 0 || s == 0 || l == 0) {
                hint.setValue("Select Sales Type, Segment and Level to list the regions.");
                updateAssignButton();
                return;
            }
            List<RegionRow> rows;
            try {
                rows = service.regionRows(l, idOf(location), t, s);
            } catch (RuntimeException ex) {
                msg.setValue("Could not load regions: " + ex.getMessage());
                updateAssignButton();
                return;
            }
            int taken = 0;
            for (RegionRow r : rows) {
                OfficerAssignment cur = r.current();
                boolean blocked = cur != null && !replace.isChecked();
                if (cur != null)
                    taken++;
                Listitem it = new Listitem();
                it.setValue(r);
                it.setDisabled(blocked);
                it.appendChild(new Listcell(r.region().name()));
                it.appendChild(new Listcell(r.path()));
                Listcell c = new Listcell();
                Label lbl = new Label(cur == null ? "Free"
                        : cur.officerName() + "  (since "
                          + cur.startDate().toLocalDateTime().toLocalDate().format(FMT) + ")");
                lbl.setStyle(cur == null ? "color:#166534;" : "color:#64748b;");
                c.appendChild(lbl);
                it.appendChild(c);
                regionList.appendChild(it);
                if (!blocked && keep.contains(Integer.valueOf(r.region().id())))
                    it.setSelected(true);
            }
            hint.setValue(rows.size() + " region(s), " + taken + " already have an officer"
                    + (taken > 0 && !replace.isChecked() ? " (tick 'Replace current officer' to change them)" : ""));
            updateAssignButton();
        }

        private Set<Integer> selectedRegionIds() {
            Set<Integer> ids = new HashSet<Integer>();
            for (Listitem it : regionList.getSelectedItems()) {
                if (!it.isDisabled() && it.getValue() instanceof RegionRow r)
                    ids.add(Integer.valueOf(r.region().id()));
            }
            return ids;
        }

        private void updateAssignButton() {
            int n = selectedRegionIds().size();
            assignBtn.setLabel(n > 0 ? "Assign (" + n + ")" : "Assign");
            assignBtn.setDisabled(n == 0);
        }

        private void onAssign() {
            msg.setValue("");
            try {
                int n = service.assign(idOf(officer), idOf(type), idOf(segment), start.getValueInLocalDate(),
                        new ArrayList<Integer>(selectedRegionIds()), replace.isChecked());
                win.detach();
                Clients.showNotification("Officer assigned to " + n + " region(s)", "info", null,
                        "top_center", 3000);
                refresh();
            } catch (ValidationException ex) {
                msg.setValue(ex.getMessage());
            } catch (RuntimeException ex) {
                msg.setValue("Could not save: " + ex.getMessage());
            }
        }
    }

    // =============================================================== small helpers

    private static Listbox select() {
        Listbox l = new Listbox();
        l.setMold("select");
        l.setStyle(INPUT);
        return l;
    }

    private static Component field(String label, Component input) {
        Div d = new Div();
        Label l = new Label(label);
        l.setStyle(FIELD_LABEL);
        d.appendChild(l);
        d.appendChild(input);
        return d;
    }

    private static Component step(String text) {
        Label l = new Label(text);
        l.setStyle("display:block; font-weight:600; color:#0f172a; margin:14px 0 8px 0;");
        return l;
    }

    private static Listheader header(String text, String width) {
        Listheader h = new Listheader(text);
        h.setWidth(width);
        return h;
    }

    private static void fill(Listbox box, List<IdName> items, String firstLabel) {
    	clearItems(box);
        box.appendChild(new Listitem(firstLabel));
        for (IdName n : items) {
            Listitem it = new Listitem(n.name());
            it.setValue(n);
            box.appendChild(it);
        }
        box.setSelectedIndex(0);
    }

    private static void fillCombo(Combobox box, List<IdName> items) {
        box.getChildren().clear();
        box.setValue("");
        for (IdName n : items) {
            Comboitem ci = new Comboitem(n.name());
            ci.setValue(n);
            box.appendChild(ci);
        }
    }
    
    private static void clearItems(Listbox box) {
        for (int i = box.getItemCount() - 1; i >= 0; i--)
            box.removeItemAt(i);
    }

    /** Location choices depend on the chosen level (the regions one level above). */
    private void reloadLocations(Listbox levelBox, Listbox locationBox, int segmentId, String firstLabel) {
        int lvl = idOf(levelBox);
        List<IdName> locs = service.locations(lvl, segmentId);
        fill(locationBox, locs, locs.isEmpty() ? "-" : firstLabel);
        locationBox.setDisabled(locs.isEmpty());
    }

    private static int idOf(Listbox box) {
        Listitem it = box.getSelectedItem();
        return it != null && it.getValue() instanceof IdName n ? n.id() : 0;
    }

    private static int idOf(Combobox box) {
        Comboitem sel = box.getSelectedItem();
        if (sel != null && sel.getValue() instanceof IdName n)
            return n.id();
        String text = box.getValue() == null ? "" : box.getValue().trim();
        if (text.isEmpty())
            return 0;
        for (Comboitem ci : box.getItems()) {
            if (text.equalsIgnoreCase(ci.getLabel()) && ci.getValue() instanceof IdName n)
                return n.id();
        }
        return 0;
    }
}
