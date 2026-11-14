package org.mycompany.o2c.model;

/** Generic id / name pair for simple lookups (organization, order type, territory). */
public class Option {
    private final int id;
    private final String name;

    public Option(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
