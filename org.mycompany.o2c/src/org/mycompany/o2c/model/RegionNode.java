package org.mycompany.o2c.model;

/**
 * One Sales Region with its place in the tree (parentId 0 = top).
 * warehouseId / warehouseName: the Depot (Warehouse) set on the region, 0 / "" when none.
 * activityId: the Segment of the region (C_Activity_ID), 0 when none.
 */
public record RegionNode(int id, String name, int parentId, int levelNo, String levelName,
        int warehouseId, String warehouseName, int activityId) {
}
