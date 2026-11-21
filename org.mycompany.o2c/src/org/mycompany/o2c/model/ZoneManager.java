package org.mycompany.o2c.model;

/** The Zone above a Territory (Base) and the Zone Manager assigned to it (userId 0 = nobody assigned). */
public record ZoneManager(int zoneId, String zoneName, int userId, String userName) {
}
