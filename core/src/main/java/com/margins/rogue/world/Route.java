package com.margins.rogue.world;

/**
 * A named run of floors — the shape a crawl descends (FR-18). The MVP holds one
 * route: 3 procedural floors, then the route ends (the authored Story Floor and
 * completion screen arrive in Stories 6.2/6.5, which extend the seam this class
 * defines). Pure model — no libGDX (AD-2). A constant singleton, so it rides
 * {@code RunState} as a transient field-initialized reference: nothing per-run
 * to persist, and a save written before this field existed loads the default
 * route (AD-6).
 */
public class Route {

    public static final Route CARAVAN_ROAD =
            new Route("The Caravan Road", 3, "The caravan road ends here.");

    private final String name;
    private final int floorCount;
    private final String endMessage;

    public Route(String name, int floorCount, String endMessage) {
        this.name = name;
        this.floorCount = floorCount;
        this.endMessage = endMessage;
    }

    public String getName() { return name; }
    public int getFloorCount() { return floorCount; }

    /** The line shown when the route's last floor is reached (the road ends). */
    public String endMessage() { return endMessage; }
}
