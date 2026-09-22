package com.phat.rbmk.multiblock;

/** Loại cột trong lõi lò. */
public enum ChannelType {
    GRAPHITE, FUEL, ROD, WATER;

    public byte id() { return (byte) ordinal(); }

    public static ChannelType byId(int id) { return values()[id]; }
}
