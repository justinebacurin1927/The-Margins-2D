package com.margins.rogue.item;

/**
 * An item stack lying on a floor tile (FR-10): a {@link Supply} ordinal plus a
 * count and grid position. Plain data — owned by {@code RunState}, serialized as
 * part of the run (AD-3/AD-6). The no-arg constructor is for libGDX Json
 * deserialization, mirroring {@code NoiseEvent}/{@code RoguePlayer}.
 */
public class FloorItem {
    public int type;
    public int count;
    public int x;
    public int y;

    private FloorItem() {} // for libGDX Json

    public FloorItem(int type, int count, int x, int y) {
        this.type = type;
        this.count = count;
        this.x = x;
        this.y = y;
    }
}
