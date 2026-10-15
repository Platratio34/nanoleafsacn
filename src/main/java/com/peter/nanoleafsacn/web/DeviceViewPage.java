package com.peter.nanoleafsacn.web;

import java.util.ArrayList;
import java.util.HashMap;

import com.peter.nanoleafsacn.Main;
import com.peter.nanoleafsacn.api.Device;
import com.peter.webserver.data.JsonSerializer;
import com.peter.webserver.web.Request;
import com.peter.webserver.web.URLPath;
import com.peter.webserver.web.WebPage;

public class DeviceViewPage extends WebPage {

    public DeviceViewPage() {
        super(URLPath.of("device","view"), "device/view.html");
        replacer = this::replace;
    }

    private HashMap<String, Object> replace(Request request) {
        HashMap<String, Object> map = new HashMap<>();
        ArrayList<String> p = request.params.getOrDefault("uuid", null);
        if(p == null || p.isEmpty()) {
            map.put("deviceName","UNKNOWN");
            map.put("uuid","");
            map.put("address","");
            map.put("layout", "[]");
            map.put("order", "[]");
            return map;
        }
        String uuid = p.getFirst();
        Device d = Main.activeDevices.getOrDefault(uuid, null);
        if(d == null) {
            map.put("deviceName","UNKNOWN");
            map.put("uuid", uuid);
            map.put("address","");
            map.put("layout", "[]");
            map.put("order", "[]");
            return map;
        }
        map.put("deviceName", d.deviceName);
        map.put("uuid", uuid);
        map.put("address",d.dmxAddress != null ? d.dmxAddress : "");
        map.put("layout", JsonSerializer.serialize(d.panelLayout));
        map.put("order", JsonSerializer.serialize(d.order));
        return map;
    }

}
