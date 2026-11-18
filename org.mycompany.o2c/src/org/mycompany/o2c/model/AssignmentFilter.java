package org.mycompany.o2c.model;

/** Filter of the main list. 0 means "all" for every id. */
public record AssignmentFilter(int docTypeId, int activityId, int officerId,
        int levelNo, int locationId, Status status) {

    public enum Status { ACTIVE, ENDED, ALL }
}
