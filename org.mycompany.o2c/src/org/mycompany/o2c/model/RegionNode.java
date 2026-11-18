package org.mycompany.o2c.model;

/** One Sales Region with its place in the tree (parentId 0 = top). */
public record RegionNode(int id, String name, int parentId, int levelNo, String levelName) {
}
