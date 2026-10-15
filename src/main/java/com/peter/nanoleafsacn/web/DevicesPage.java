package com.peter.nanoleafsacn.web;

import java.io.FileNotFoundException;
import java.util.HashMap;

import com.peter.nanoleafsacn.Main;
import com.peter.nanoleafsacn.api.Device;
import com.peter.webserver.data.JsonObj;
import com.peter.webserver.web.Request;
import com.peter.webserver.web.Responses;
import com.peter.webserver.web.URLPath;
import com.peter.webserver.web.WebEndpoint;
import com.peter.webserver.web.http.HttpResponse;
import com.peter.webserver.web.http.Method;
import com.peter.webserver.web.http.ResponseCode;

public class DevicesPage implements WebEndpoint {

    public static final URLPath PATH = URLPath.of("devices");

    @Override
    public URLPath getPath() {
        return PATH;
    }

    @Override
    public boolean isValid(URLPath path) {
        return path.equals(PATH);
    }

    @Override
    public HttpResponse process(Request request) {
        if (request.method.equals(Method.POST)) {
            JsonObj req = request.bodyJson();
            String uuid = req.getString("uuid", null);
            if (uuid == null) {
                return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Missing uuid");
            } else if (!Main.discoveryListener.knownDevices.containsKey(uuid)) {
                return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Unknown Device");
            }
            String action = req.getString("action", null);
            if (action == null) {
                return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Missing action");
            }
            switch (action) {
                case "identify" -> {
                    Device d = Main.discoveryListener.knownDevices.get(uuid);
                    if (!d.identify()) {
                        return HttpResponse.plainText(ResponseCode.INTERNAL_SERVER_ERROR, "Failed to identify device");
                    }
                    return HttpResponse.plainText(ResponseCode.OK, "Identifying device");
                }
                case "add" -> {
                    Device d = Main.discoveryListener.knownDevices.get(uuid);
                    if (!d.getAuth()) {
                        return HttpResponse.plainText(ResponseCode.CONFLICT, "Unable to add device");
                    }
                    Main.addActiveDevice(d);
                    return HttpResponse.plainText(ResponseCode.OK, "Device added");
                }
                case "remove" -> {
                    Device d = Main.discoveryListener.knownDevices.get(uuid);
                    if (!d.clearAuth()) {
                        return HttpResponse.plainText(ResponseCode.CONFLICT, "Unable to remove device");
                    }
                    Main.removeActiveDevice(d);
                    return HttpResponse.plainText(ResponseCode.OK, "Device removed");
                }
                case "update" -> {
                    Device d = Main.discoveryListener.knownDevices.get(uuid);
                    if (!d.getLayout()) {
                        return HttpResponse.plainText(ResponseCode.CONFLICT, "Unable to update device layout");
                    }
                    Main.saveDevices();
                    return HttpResponse.plainText(ResponseCode.OK, "Device layout updated");
                }
                default -> {
                    return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Unknown action");
                }
            }
        }
        HashMap<String, Object> replacer = new HashMap<>();
        JsonObj knownDevices = JsonObj.ofArray();
        for (Device d : Main.discoveryListener.knownDevices.values()) {
            knownDevices.addArray(d.toJson());
        }
        if (request.params.containsKey("list")) {
            return HttpResponse.json(knownDevices);
        }
        replacer.put("knownDevices", knownDevices);
        try {
            return Responses.htmlPath(request, "html/devices.html", replacer);
        } catch (FileNotFoundException e) {
            System.err.println("Error loading add device html");
            e.printStackTrace();
            return HttpResponse.plainText(ResponseCode.INTERNAL_SERVER_ERROR, "Something went wrong loading page");
        }
    }

}
