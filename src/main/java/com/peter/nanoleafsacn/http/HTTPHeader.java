package com.peter.nanoleafsacn.http;

import java.util.ArrayList;

public class HTTPHeader {

    public final String name;
    public final ArrayList<String> values = new ArrayList<>();

    protected HTTPHeader(String name) {
        this.name = name;
    }

    protected HTTPHeader(String name, String value) {
        this.name = name;
        values.add(value);
    }

    public void add(String value) {
        values.add(value);
    }

    @Override
    public String toString() {
        String out = "HTTPHeader{name=\"" + name + "\", values=";
        if (values.size() == 1) {
            return out += String.format("\"%s\"}", values.getFirst());
        }
        out += "[";
        boolean f = true;
        for (String v : values) {
            if(!f)
                out += ",";
            out += String.format("\"%s\"", v);
            f = false;
        }
        return out + "]}";
    }
    
    public String toHTTP() {
        String out = "";
        for (String v : values) {
            out += String.format("%s: %s\n", name, v);
        }
        return out;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof String str) {
            if (values.isEmpty())
                return false;
            return values.contains(str);
        }
        return super.equals(obj);
    }

    public String get() {
        if (values.isEmpty()) {
            return "";
        }
        return values.getFirst();
    }
}
