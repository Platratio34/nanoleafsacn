package com.peter.nanoleafsacn.sacn;

public class SacnAddress {

    public final int universe;
    public final int start;

    public SacnAddress(int start) {
        this(1, start);
    }

    public SacnAddress(int universe, int start) {
        if (universe < 1) {
            universe = 1;
        } else if (universe > 0xffff) {
            universe = 0xffff;
        }
        if (start < 1) {
            start = 1;
        } else if (start > 512) {
            start = 512;
        }
        this.universe = universe;
        this.start = start;
    }

    public SacnAddress at(int start) {
        return new SacnAddress(universe, start);
    }

    public SacnAddress in(int universe) {
        return new SacnAddress(universe, start);
    }
    
    @Override
    public String toString() {
        return String.format("%d/%d", universe, start);
    }

    public static SacnAddress from(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        String[] parts = address.split("/");
        if (parts.length == 1) {
            try {
                return new SacnAddress(Integer.parseInt(parts[0]));
            } catch (NumberFormatException e) {

            }
        } else {
            int universe = 1;
            try {
                universe = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {

            }
            try {
                return new SacnAddress(universe, Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {

            }
        }
        return new SacnAddress(1);
    }
}
