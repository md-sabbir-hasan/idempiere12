package org.mycompany.o2c.model;

/** A region shown in the Create popup, with the officer it has right now (null = free). */
public record RegionRow(RegionNode region, String path, OfficerAssignment current) {
}
