package com.peter.nanoleafsacn.web;

import java.util.HashMap;
import java.util.Map.Entry;

import com.peter.nanoleafsacn.Main;
import com.peter.nanoleafsacn.sacn.SacnSource;
import com.peter.nanoleafsacn.sacn.SacnUniverse;
import com.peter.webserver.data.JsonObj;
import com.peter.webserver.web.APIEndpoint;
import com.peter.webserver.web.Request;
import com.peter.webserver.web.URLPath;
import com.peter.webserver.web.http.HttpResponse;
import com.peter.webserver.web.http.ResponseCode;
import com.peter.webserver.web.sse.SseChannel;

public class SacnAPI extends APIEndpoint {

    public SacnAPI() {
        super(URLPath.of("api", "sacn"));
        Main.updateThread.listeners.add(this::updateSse);
    }

    public final HashMap<Integer, SseChannel> dataChannels = new HashMap<>();
    
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
                if (request.path.length() == 5) {
                    if (request.path.get(4).equals("sse")) {
                        if (!dataChannels.containsKey(universe))
                            dataChannels.put(universe, new SseChannel());
                        return dataChannels.get(universe).newResponse();
                    }
                }
                SacnUniverse uni = Main.sacnListener.getUniverse(universe);
                return HttpResponse.json(uniToJson(uni));
            }
        }
        return HttpResponse.plainText(ResponseCode.BAD_REQUEST, "Unknown action `%s`", action);
    }

    private static JsonObj uniToJson(SacnUniverse uni) {
        JsonObj uniJ = JsonObj.ofObject();
        JsonObj dataJ = JsonObj.ofArray();
        uniJ.setKey("data", dataJ);
        JsonObj prioJ = JsonObj.ofArray();
        uniJ.setKey("prio", prioJ);
        for (int i = 0; i < uni.data.length; i++) {
            dataJ.addArray(uni.data[i] & 0xff);
            prioJ.addArray(uni.priority[i] & 0xff);
        }
        return uniJ;
    }
    
    public void updateSse(int i) {
        if (i % 5 != 0)
            return;
        for (Entry<Integer, SseChannel> entry : dataChannels.entrySet()) {
            SseChannel channel = entry.getValue();
            if(!channel.hasListeners())
                continue;
            int u = (int)entry.getKey();
            JsonObj data = uniToJson(Main.sacnListener.getUniverse(u));
            channel.event("data", data.toStringC());
        }
    }

}
