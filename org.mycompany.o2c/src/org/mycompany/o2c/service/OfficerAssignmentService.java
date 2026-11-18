package org.mycompany.o2c.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.mycompany.o2c.dao.OfficerAssignmentDAO;
import org.mycompany.o2c.model.AssignmentFilter;
import org.mycompany.o2c.model.AssignmentFilter.Status;
import org.mycompany.o2c.model.IdName;
import org.mycompany.o2c.model.OfficerAssignment;
import org.mycompany.o2c.model.RegionNode;
import org.mycompany.o2c.model.RegionRow;
import org.mycompany.o2c.util.ValidationException;

/**
 * Rules of the officer assignment:
 *  - one current officer (End Date empty) per Region + Sales Type + Segment
 *  - to change the officer, the old one gets an End Date (the day before the new Start Date)
 *  - history is never deleted
 */
public class OfficerAssignmentService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String SEP = " › ";

    private final OfficerAssignmentDAO dao = new OfficerAssignmentDAO();
    private Map<Integer, RegionNode> regions;

    // ---------------------------------------------------------------- dropdown data

    public List<IdName> salesTypes() {
        return dao.findSalesTypes();
    }

    public List<IdName> segments() {
        return dao.findSegments();
    }

    public List<IdName> officers() {
        return dao.findOfficers();
    }

    /** Levels found in the Sales Region data, id = level number, text like "2-Division". */
    public List<IdName> levels() {
        TreeMap<Integer, String> found = new TreeMap<Integer, String>();
        for (RegionNode r : regionMap().values()) {
            if (r.levelNo() > 0 && !found.containsKey(Integer.valueOf(r.levelNo())))
                found.put(Integer.valueOf(r.levelNo()), r.levelName());
        }
        List<IdName> out = new ArrayList<IdName>();
        for (Map.Entry<Integer, String> e : found.entrySet())
            out.add(new IdName(e.getKey().intValue(), e.getKey() + "-" + e.getValue()));
        return out;
    }

    /** Locations to pick for a level = the regions one level above it. Empty for level 1. */
    public List<IdName> locations(int levelNo) {
        List<IdName> out = new ArrayList<IdName>();
        if (levelNo <= 1)
            return out;
        for (RegionNode r : regionMap().values()) {
            if (r.levelNo() == levelNo - 1) {
                String above = r.parentId() > 0 ? path(r.parentId()) : "";
                out.add(new IdName(r.id(), above.isEmpty() ? r.name() : r.name() + " (" + above + ")"));
            }
        }
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    /** "Bangladesh > Dhaka > Dhaka North > Gazipur" */
    public String path(int regionId) {
        StringBuilder sb = new StringBuilder();
        int id = regionId;
        for (int depth = 0; id > 0 && depth < 20; depth++) {
            RegionNode r = regionMap().get(Integer.valueOf(id));
            if (r == null)
                break;
            sb.insert(0, sb.length() == 0 ? r.name() : r.name() + SEP);
            id = r.parentId();
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- main list

    public List<OfficerAssignment> search(AssignmentFilter f) {
        List<OfficerAssignment> all = dao.findAssignments(f.docTypeId(), f.activityId(), f.officerId(), f.status());
        if (f.levelNo() <= 0 && f.locationId() <= 0)
            return all;
        List<OfficerAssignment> out = new ArrayList<OfficerAssignment>();
        for (OfficerAssignment a : all) {
            RegionNode r = regionMap().get(Integer.valueOf(a.regionId()));
            if (r == null)
                continue;
            if (f.levelNo() > 0 && r.levelNo() != f.levelNo())
                continue;
            if (f.locationId() > 0 && !isUnder(r.id(), f.locationId()))
                continue;
            out.add(a);
        }
        return out;
    }

    // ---------------------------------------------------------------- create popup

    /** Regions of a level (below the Location when given) and the officer each one has now. */
    public List<RegionRow> regionRows(int levelNo, int locationId, int docTypeId, int activityId) {
        List<RegionRow> out = new ArrayList<RegionRow>();
        if (levelNo <= 0)
            return out;
        Map<Integer, OfficerAssignment> current = new HashMap<Integer, OfficerAssignment>();
        if (docTypeId > 0 && activityId > 0) {
            for (OfficerAssignment a : dao.findAssignments(docTypeId, activityId, 0, Status.ACTIVE))
                current.put(Integer.valueOf(a.regionId()), a);
        }
        for (RegionNode r : regionMap().values()) {
            if (r.levelNo() != levelNo)
                continue;
            if (locationId > 0 && r.parentId() != locationId)
                continue;
            out.add(new RegionRow(r, path(r.id()), current.get(Integer.valueOf(r.id()))));
        }
        out.sort((a, b) -> a.path().compareToIgnoreCase(b.path()));
        return out;
    }

    /** Assigns the officer to every region. Returns how many regions were assigned. */
    public int assign(int officerId, int docTypeId, int activityId, LocalDate start,
            List<Integer> regionIds, boolean replaceCurrent) {
        if (docTypeId <= 0)
            throw new ValidationException("Select the Sales Type.");
        if (activityId <= 0)
            throw new ValidationException("Select the Segment.");
        if (regionIds == null || regionIds.isEmpty())
            throw new ValidationException("Select at least one region.");
        if (officerId <= 0)
            throw new ValidationException("Select the Officer.");
        if (start == null)
            throw new ValidationException("Enter the Start Date.");

        Map<Integer, List<OfficerAssignment>> history = new HashMap<Integer, List<OfficerAssignment>>();
        for (OfficerAssignment a : dao.findAssignments(docTypeId, activityId, 0, Status.ALL))
            history.computeIfAbsent(Integer.valueOf(a.regionId()), k -> new ArrayList<OfficerAssignment>()).add(a);

        List<Integer> toEnd = new ArrayList<Integer>();
        for (Integer rid : regionIds) {
            for (OfficerAssignment a : history.getOrDefault(rid, Collections.<OfficerAssignment>emptyList())) {
                LocalDate aStart = a.startDate().toLocalDateTime().toLocalDate();
                if (a.isOpen()) {
                    if (!replaceCurrent)
                        throw new ValidationException(a.regionName() + " already has the officer "
                            + a.officerName() + " (since " + aStart.format(FMT)
                            + "). Tick 'Replace current officer', or end that assignment first.");
                    if (!start.isAfter(aStart))
                        throw new ValidationException(a.regionName() + ": Start Date must be after "
                            + aStart.format(FMT) + " (Start Date of " + a.officerName() + ").");
                    toEnd.add(Integer.valueOf(a.id()));
                } else {
                    LocalDate aEnd = a.endDate().toLocalDateTime().toLocalDate();
                    if (!aEnd.isBefore(start))
                        throw new ValidationException(a.regionName() + ": an earlier assignment ends on "
                            + aEnd.format(FMT) + ". Start Date must be after it.");
                }
            }
        }
        dao.assign(officerId, docTypeId, activityId, Timestamp.valueOf(start.atStartOfDay()), regionIds,
                toEnd, Timestamp.valueOf(start.minusDays(1).atStartOfDay()));
        return regionIds.size();
    }

    /** Gives a current assignment its End Date. */
    public void end(int assignmentId, LocalDate endDate) {
        if (endDate == null)
            throw new ValidationException("Enter the End Date.");
        OfficerAssignment a = dao.findById(assignmentId);
        if (a == null)
            throw new ValidationException("The assignment was not found. Refresh the list.");
        if (!a.isOpen())
            throw new ValidationException("This assignment is already ended.");
        LocalDate aStart = a.startDate().toLocalDateTime().toLocalDate();
        if (endDate.isBefore(aStart))
            throw new ValidationException("End Date cannot be before the Start Date (" + aStart.format(FMT) + ").");
        if (!dao.end(assignmentId, Timestamp.valueOf(endDate.atStartOfDay())))
            throw new ValidationException("This assignment is already ended. Refresh the list.");
    }

    // ---------------------------------------------------------------- helpers

    private boolean isUnder(int regionId, int ancestorId) {
        int id = regionId;
        for (int depth = 0; id > 0 && depth < 20; depth++) {
            RegionNode r = regionMap().get(Integer.valueOf(id));
            if (r == null)
                return false;
            if (r.parentId() == ancestorId)
                return true;
            id = r.parentId();
        }
        return false;
    }

    private Map<Integer, RegionNode> regionMap() {
        if (regions == null) {
            Map<Integer, RegionNode> m = new HashMap<Integer, RegionNode>();
            for (RegionNode r : dao.findRegions())
                m.put(Integer.valueOf(r.id()), r);
            regions = m;
        }
        return regions;
    }
}
