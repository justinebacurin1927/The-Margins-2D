package com.margins.item;

import java.util.Arrays;

public class Inventory {
    private static final int SLOTS = 16;
    private final int[] types;
    private final int[] counts;

    public Inventory() {
        types = new int[SLOTS];
        counts = new int[SLOTS];
        Arrays.fill(types, -1);
    }

    public boolean add(int type, int amount) {
        for (int i = 0; i < SLOTS; i++) {
            if (types[i] == type) {
                counts[i] += amount;
                return true;
            }
        }
        for (int i = 0; i < SLOTS; i++) {
            if (types[i] == -1) {
                types[i] = type;
                counts[i] = amount;
                return true;
            }
        }
        return false;
    }

    public boolean remove(int type, int amount) {
        for (int i = 0; i < SLOTS; i++) {
            if (types[i] == type) {
                if (counts[i] >= amount) {
                    counts[i] -= amount;
                    if (counts[i] == 0) types[i] = -1;
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public int count(int type) {
        for (int i = 0; i < SLOTS; i++) {
            if (types[i] == type) return counts[i];
        }
        return 0;
    }

    public boolean has(int type) { return count(type) > 0; }

    public void setSlot(int slot, int type, int count) {
        if (slot >= 0 && slot < SLOTS) {
            types[slot] = type;
            counts[slot] = count;
        }
    }

    public int slots() { return SLOTS; }
    public int getType(int slot) { return types[slot]; }
    public int getCount(int slot) { return counts[slot]; }
}
