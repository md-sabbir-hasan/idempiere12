package org.mycompany.o2c.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What the logged in Sales Representative may sell, read from the Officer Assignment:
 * one Segment, and the Territories (Base regions) with their Sales Type and Depot.
 */
public record OfficerScope(int officerId, String officerName, int segmentId, String segmentName,
        List<Territory> territories) {

    /** One assigned Territory. Depot = Warehouse of the region (or of its nearest parent). */
    public record Territory(int regionId, String name, String path, int docTypeId, String docTypeName,
            int warehouseId, String warehouseName) {
    }

    /** Sales Types (Document Types) of the officer, each once. */
    public List<IdName> salesTypes() {
        Set<Integer> seen = new LinkedHashSet<Integer>();
        List<IdName> out = new ArrayList<IdName>();
        for (Territory t : territories) {
            if (seen.add(Integer.valueOf(t.docTypeId())))
                out.add(new IdName(t.docTypeId(), t.docTypeName()));
        }
        return out;
    }

    public List<Territory> territoriesFor(int docTypeId) {
        List<Territory> out = new ArrayList<Territory>();
        for (Territory t : territories) {
            if (t.docTypeId() == docTypeId)
                out.add(t);
        }
        return out;
    }
}
