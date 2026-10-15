package com.peter.nanoleafsacn.web;

import com.peter.nanoleafsacn.Main;
import com.peter.nanoleafsacn.sacn.SacnSource;
import com.peter.nanoleafsacn.sacn.SacnUniverse;
import com.peter.webserver.data.JsonObj;
import com.peter.webserver.web.APIEndpoint;
import com.peter.webserver.web.Request;
import com.peter.webserver.web.URLPath;
import com.peter.webserver.web.http.HttpResponse;
import com.peter.webserver.web.http.ResponseCode;

public class SacnAPI extends APIEndpoint {

    public SacnAPI() {
        super(URLPath.of("api", "sacn"));
    }
    
    @Override
    public HttpResponse process(Request request) {
        if (request.path.length() < 3) {
            return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Missing action");
        }
        String action = request.path.get(2);
        switch (action) {
            case "sources" -> {
                JsonObj obj = JsonObj.ofObject();
                for (SacnSource src : Main.sacnListener.sources.values()) {
                    JsonObj srcJ = JsonObj.ofObject();
                    obj.setKey(src.name, srcJ);
                    JsonObj uniArr = JsonObj.ofArray();
                    srcJ.setKey("universes", uniArr);
                    for (Integer i : src.universes.keySet()) {
                        uniArr.addArray(i);
                    }
                }
                return HttpResponse.json(obj);
            }
            case "universe" -> {
                if (request.path.length() < 4) {
                    return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Missing universe");
                }
                int universe;
                try {
                    universe = Integer.parseInt(request.path.get(3));
                } catch (NumberFormatException e) {
                    return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Malformed universe");
                }
                if (universe < 1 || universe > 0xffff) {
                    return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Invalid universe");
                }
                SacnUniverse uni = Main.sacnListener.getUniverse(universe);
                JsonObj uniJ = JsonObj.ofObject();
                JsonObj dataJ = JsonObj.ofArray();
                uniJ.setKey("data", dataJ);
                JsonObj prioJ = JsonObj.ofArray();
                uniJ.setKey("prio", prioJ);
                for (int i = 0; i < uni.data.length; i++) {
                    dataJ.addArray(uni.data[i] & 0xff);
                    prioJ.addArray(uni.priority[i] & 0xff);
                }
                return HttpResponse.json(uniJ);
            }
        }
        return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Unknown action `%s`", action);
    }

}
