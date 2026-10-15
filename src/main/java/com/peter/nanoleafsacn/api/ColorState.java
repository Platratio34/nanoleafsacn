package com.peter.nanoleafsacn.api;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import com.peter.webserver.data.JsonObj;

public class ColorState {

    public final Device device;

    HashMap<Integer, Color> colors = new HashMap<>();

    public ColorState(Device device) {
        this.device = device;
    }

    public void setColor(int panelId, Color color) {
        colors.put(panelId, color);
    }

    public JsonObj build() {
        return build(1);
    }

    public JsonObj build(int time) {
        String animData = "";
        animData += colors.size();
        for (Entry<Integer, Color> entry : colors.entrySet()) {
            Color color = entry.getValue();
            animData += String.format(" %d 1 %d %d %d 0 %d", entry.getKey(), color.red, color.green, color.blue, time);
        }
        JsonObj json = JsonObj.ofObject();
        // System.out.println(animData);
        json.setKey("version", "2.0");
        json.setKey("command", "display");
        json.setKey("animType", "custom");
        json.setKey("animData", animData);
        json.setKey("loop", false);
        json.setKey("pallet", JsonObj.ofArray());
        json.setKey("colorType", "RGB");
        return json;
    }

    public void buildStream(OutputStream stream) {
        // byte[] bytes = {}
    }
}
