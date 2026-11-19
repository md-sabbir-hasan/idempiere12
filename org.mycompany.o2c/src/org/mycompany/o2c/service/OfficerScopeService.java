package org.mycompany.o2c.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mycompany.o2c.dao.OfficerAssignmentDAO;
import org.mycompany.o2c.model.OfficerAssignment;
import org.mycompany.o2c.model.OfficerScope;
import org.mycompany.o2c.model.OfficerScope.Territory;
import org.mycompany.o2c.model.RegionNode;
import org.mycompany.o2c.util.ValidationException;

/**
 * What the logged in Sales Representative may sell (Sales Order form):
 * Segment, Sales Types, Territories and the Depot of every Territory, all from the Officer Assignment.
 */
public class OfficerScopeService {
    private static final String SEP = " › ";

    private final OfficerAssignmentDAO dao = new OfficerAssignmentDAO();

    /**
     * @throws ValidationException (user friendly text) when the user has no current assignment,
     *         or is assigned to more than one Segment.
     */
    public OfficerScope load(int userId, LocalDate on) {
        if (userId <= 0)
            throw new ValidationException("The logged in user was not found.");

        List<OfficerAssignment> mine = dao.findCurrentForOfficer(userId, Timestamp.valueOf(on.atStartOfDay()));
        if (mine.isEmpty())
            throw new ValidationException("You have no current Officer Assignment, so you cannot create "
                + "a Sales Order. Log in as a Sales Representative user, or ask your manager to assign "
                + "this user (ID " + userId + ") in Officer Assignment.");

        Map<Integer, String> segments = new LinkedHashMap<Integer, String>();
        for (OfficerAssignment a : mine)
            segments.put(Integer.valueOf(a.activityId()), a.activityName());
        if (segments.size() > 1)
            throw new ValidationException("You are assigned to more than one Segment ("
                + String.join(", ", segments.values()) + "). Only one Segment is allowed. "
                + "Correct it in Officer Assignment.");

        Map<Integer, RegionNode> regions = new HashMap<Integer, RegionNode>();
        for (RegionNode r : dao.findRegions())
            regions.put(Integer.valueOf(r.id()), r);

        List<Territory> territories = new ArrayList<Territory>();
        for (OfficerAssignment a : mine) {
            RegionNode r = regions.get(Integer.valueOf(a.regionId()));
            if (r == null)
                continue;
            RegionNode withDepot = depotOwner(r, regions);
            territories.add(new Territory(r.id(), r.name(), path(r, regions), a.docTypeId(), a.docTypeName(),
                    withDepot == null ? 0 : withDepot.warehouseId(),
                    withDepot == null ? "" : withDepot.warehouseName()));
        }
        territories.sort((x, y) -> {
            int c = x.docTypeName().compareToIgnoreCase(y.docTypeName());
            return c != 0 ? c : x.name().compareToIgnoreCase(y.name());
        });

        Map.Entry<Integer, String> seg = segments.entrySet().iterator().next();
        return new OfficerScope(userId, mine.get(0).officerName(), seg.getKey().intValue(), seg.getValue(),
                territories);
    }

    /** The region itself when it has a Depot, otherwise its nearest parent that has one. */
    private static RegionNode depotOwner(RegionNode start, Map<Integer, RegionNode> regions) {
        RegionNode r = start;
        for (int depth = 0; r != null && depth < 20; depth++) {
            if (r.warehouseId() > 0)
                return r;
            r = regions.get(Integer.valueOf(r.parentId()));
        }
        return null;
    }

    private static String path(RegionNode start, Map<Integer, RegionNode> regions) {
        StringBuilder sb = new StringBuilder();
        RegionNode r = start;
        for (int depth = 0; r != null && depth < 20; depth++) {
            sb.insert(0, sb.length() == 0 ? r.name() : r.name() + SEP);
            r = regions.get(Integer.valueOf(r.parentId()));
        }
        return sb.toString();
    }
}
