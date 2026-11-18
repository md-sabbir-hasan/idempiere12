package org.mycompany.o2c.model;

/** A dropdown item: database id + text shown to the user. */
public record IdName(int id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
