package com.peter.nanoleafsacn.sacn;

import java.util.HashMap;

import com.peter.nanoleafsacn.Main;

public class SacnSource {

    public final String name;

    public HashMap<Integer, SacnUniverse> universes = new HashMap<>();

    public SacnSource(String name) {
        this.name = name;
    }

    public void update(SacnPacket sacnPacket) {
        // Main.LOGGER.info("Updating universe {} for source {}", sacnPacket.universe, name);
        if(sacnPacket.startCode == 0x00)
            universes.put(sacnPacket.universe, SacnUniverse.from(sacnPacket));
    }

    public SacnUniverse get(int universe) {
        if (!universes.containsKey(universe)) {
            return null;
        }
        SacnUniverse uni = universes.get(universe);
        if (System.currentTimeMillis() - uni.time > 10000) {
            universes.remove(universe);
            // Main.LOGGER.info("Source timed out for universe {}", universe);
            return null;
        }
        // Main.LOGGER.info("Got universe {} from source {}", universe, name);
        return uni;
    }

}
