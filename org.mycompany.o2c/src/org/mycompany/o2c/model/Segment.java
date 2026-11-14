package org.mycompany.o2c.model;

/** Business segment (XX_Segment). */
public class Segment {
    private final int id;
    private final String name;

    public Segment(int id, String name) {
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
