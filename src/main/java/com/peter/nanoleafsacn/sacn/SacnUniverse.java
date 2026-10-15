package com.peter.nanoleafsacn.sacn;

public class SacnUniverse {

    public final int universe;
    public final byte[] data;
    public final byte[] priority = new byte[512];
    public final long time;

    public SacnUniverse(int universe, long time) {
        this.universe = universe;
        data = new byte[512];
        this.time = time;
    }
    public SacnUniverse(int universe, byte[] data, long time) {
        this.universe = universe;
        this.data = data;
        this.time = time;
    }
    public SacnUniverse(int universe, byte[] data, byte priority, long time) {
        this.universe = universe;
        this.data = data;
        this.time = time;
        for (int i = 0; i < this.priority.length; i++) {
            this.priority[i] = priority;
        }
    }

    public void add(SacnUniverse universe) {
        // String out = "";
        for (int i = 0; i < data.length; i++) {
            int priorityDifference = (universe.priority[i] & 0xff) - (priority[i] & 0xff);
            if (priorityDifference < 0 || (priorityDifference == 0 && (universe.data[i] & 0xff) <= (data[i] & 0xff))) {
                // out += String.format(" -%02x %03d", data[i], priorityDifference);
                continue;
            }
            data[i] = universe.data[i];
            priority[i] = universe.priority[i];
            // out += String.format(" +%02x %02x", data[i], priority[i]);
        }
        // Main.LOGGER.debug(out);
    }

    public static SacnUniverse from(SacnPacket sacnPacket) {
        SacnUniverse universe;
        if (sacnPacket.data.length == 512) {
            return new SacnUniverse(sacnPacket.universe, sacnPacket.data, sacnPacket.priority, System.currentTimeMillis());
        }
        universe = new SacnUniverse(sacnPacket.universe, System.currentTimeMillis());
        for (int i = 0; i < sacnPacket.data.length; i++) {
            universe.data[i] = sacnPacket.data[i];
            universe.priority[i] = sacnPacket.priority;
        }
        return universe;
    }
}
