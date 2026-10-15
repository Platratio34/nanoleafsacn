package com.peter.nanoleafsacn.web;

import com.peter.nanoleafsacn.Main;
import com.peter.nanoleafsacn.api.Color;
import com.peter.nanoleafsacn.api.ColorState;
import com.peter.nanoleafsacn.api.Device;
import com.peter.nanoleafsacn.sacn.SacnAddress;
import com.peter.webserver.data.JsonObj;
import com.peter.webserver.data.JsonSerializable;
import com.peter.webserver.data.JsonSerializer;
import com.peter.webserver.web.APIEndpoint;
import com.peter.webserver.web.Request;
import com.peter.webserver.web.URLPath;
import com.peter.webserver.web.http.HttpResponse;
import com.peter.webserver.web.http.Method;
import com.peter.webserver.web.http.ResponseCode;

public class DeviceAPI extends APIEndpoint {

    public DeviceAPI() {
        super(URLPath.of("api","device"));
    }

    @Override
    public HttpResponse process(Request request) {
        if (request.path.length() < 4) {
            return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Missing uuid and/or action");
        }
        String uuid = request.path.get(2);
        Device d = Main.activeDevices.getOrDefault(uuid, null);
        if (d == null) {
            return HttpResponse.plainText(ResponseCode.NOT_FOUND, "No connected device with uuid `%s`", uuid);
        }
        String action = request.path.get(3);
        boolean isPost = request.method.equals(Method.POST);
        switch (action) {
            case "setState" -> {
                if (!isPost) {
                    return HttpResponse.plainText(ResponseCode.METHOD_NOT_ALLOWED, "`setState` must be POST");
                }
                JsonObj body = request.bodyJson().getKey("state");
                ColorState state = d.buildColorState();
                for (JsonObj p : body.getArr()) {
                    state.setColor(p.getInt("panelId"), new Color(p));
                    request.server.logger.info("- setting state for {}", p.getInt("panelId"));
                }
                if (!d.setEffect(state.build())) {
                    return HttpResponse.plainText(ResponseCode.INTERNAL_SERVER_ERROR, "Failed to set state");
                }
                return HttpResponse.plainText("State set");
            }
            case "setAddress" -> {
                if (!isPost) {
                    return HttpResponse.plainText(ResponseCode.METHOD_NOT_ALLOWED, "`setAddress` must be POST");
                }
                d.dmxAddress = SacnAddress.from(request.bodyJson().getString("address"));
                Main.saveDevices();
                return HttpResponse.plainText("Address set");
            }
            case "setOrder" -> {
                if (!isPost) {
                    return HttpResponse.plainText(ResponseCode.METHOD_NOT_ALLOWED, "`setOrder` must be POST");
                }
                JsonObj[] arr = request.bodyJson().getKey("order").getArr();
                d.order = new Integer[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    d.order[i] = arr[i].integer();
                }
                Main.saveDevices();
                return HttpResponse.plainText("Order set");
            }
        }
        return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Unknown action `%s`", action);
    }

}
