package com.peter.nanoleafsacn.api;

import java.io.IOException;

import com.peter.nanoleafsacn.Main;
import com.peter.nanoleafsacn.api.LayoutPacket.ShapeType;
import com.peter.nanoleafsacn.http.HTTPMessage;
import com.peter.nanoleafsacn.sacn.SacnAddress;
import com.peter.nanoleafsacn.sacn.SacnUniverse;
import com.peter.webserver.data.JsonObj;
import com.peter.webserver.data.JsonSerializer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Device {

    private static final MediaType MIME_APPLICATION_JSON = MediaType.get("application/json");

    public final String uuid;
    public String deviceID = null;
    public String deviceName = null;
    public String address = null;

    public boolean active = false;
    public boolean connected = false;

    public String authKey = null;

    public SacnAddress dmxAddress = null;

    public Integer[] order = new Integer[0];

    protected OkHttpClient httpClient = new OkHttpClient();

    public Device(String uuid) {
        this.uuid = uuid;
    }

    public JsonObj toJson() {
        JsonObj json = JsonObj.ofObject();
        json.setKey("uuid", uuid);
        json.setKey("address", address);
        json.setKey("deviceName", deviceName);
        json.setKey("deviceID", deviceID);
        json.setKey("active", active);
        json.setKey("layout", JsonSerializer.serialize(panelLayout));
        if (dmxAddress != null)
            json.setKey("dmxAddress", dmxAddress.toString());
        json.setKey("order", JsonSerializer.serialize(order));
        return json;
    }

    public JsonObj toJsonSave() {
        JsonObj json = toJson();
        json.setKey("authKey", authKey);
        return json;
    }
    
    public static Device fromJson(JsonObj json) {
        Device d = new Device(json.getString("uuid"));
        d.address = json.getString("address", null);
        d.deviceName = json.getString("deviceName", null);
        d.deviceID = json.getString("deviceID", null);
        d.authKey = json.getString("authKey", null);
        d.active = json.getBool("active", false);
        if(json.hasKey("layout"))
            d.panelLayout = JsonSerializer.deserialize(LayoutPacket.PositionLayout[].class, json.getKey("layout"));
        if (json.hasKey("dmxAddress")) {
            d.dmxAddress = SacnAddress.from(json.getString("dmxAddress"));
        }
        if (json.hasKey("order")) {
            JsonObj[] arr = json.getKey("order").getArr();
            d.order = new Integer[arr.length];
            for (int i = 0; i < arr.length; i++) {
                d.order[i] = arr[i].integer();
            }
        }
        return d;
    }

    public void update(HTTPMessage ssdpMessage) {
        address = ssdpMessage.getHeader("Location").get().substring(7);
        deviceID = ssdpMessage.getHeader("nl-deviceid").get();
        deviceName = ssdpMessage.getHeader("nl-devicename").get();
        Main.saveDevices();
    }

    public void markUnavailable() {
        connected = false;
    }
    
    public boolean getAuth() {
        if (address == null) {
            return false;
        }
        Request req = new Request.Builder()
                .url("http://" + address + "/api/v1/new")
                .addHeader("User-Agent", "Nanoleaf Sacn")
                .post(RequestBody.create("", MediaType.get("text/plain")))
                .build();
        try (Response rsp = httpClient.newCall(req).execute()) {
            if (rsp.code() == 200) {
                AuthPacket packet = JsonSerializer.deserialize(AuthPacket.class, JsonObj.parseD(rsp.body().string()));
                authKey = packet.auth_token;
                connected = true;
                return true;
            }
            System.err.println("Error getting auth token: " + rsp.code());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean clearAuth() {
        if (address == null) {
            return false;
        }
        if (authKey == null) {
            return true;
        }
        Request req = new Request.Builder()
                .url("http://" + address + "/api/v1/" + authKey)
                .addHeader("User-Agent", "Nanoleaf Sacn")
                .delete()
                .build();
        try (Response rsp = httpClient.newCall(req).execute()) {
            if (rsp.code() == 204 || rsp.code() == 401) {
                authKey = null;
                return true;
            }
            System.err.println("Error clearing auth token: " + rsp.code());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public LayoutPacket.PositionLayout[] panelLayout = null;

    public boolean getInfo() {
        if (address == null || authKey == null) {
            return false;
        }
        Request req = new Request.Builder()
                .url("http://" + address + "/api/v1/" + authKey + "/state")
                .addHeader("User-Agent", "Nanoleaf Sacn")
                .get()
                .build();
        try (Response rsp = httpClient.newCall(req).execute()) {
            if (rsp.code() == 200) {
                LayoutPacket packet = JsonSerializer.deserialize(LayoutPacket.class,
                        JsonObj.parseD(rsp.body().string()));
                panelLayout = packet.positionData;
                return true;
            }
            System.err.println("Error getting device info: " + rsp.code());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean getLayout() {
        if (address == null || authKey == null) {
            return false;
        }
        Request req = new Request.Builder()
                .url("http://" + address + "/api/v1/"+authKey+"/panelLayout/layout")
                .addHeader("User-Agent", "Nanoleaf Sacn")
                .get()
                .build();
        try (Response rsp = httpClient.newCall(req).execute()) {
            if (rsp.code() == 200) {
                LayoutPacket packet = JsonSerializer.deserialize(LayoutPacket.class,
                        JsonObj.parseD(rsp.body().string()));
                panelLayout = packet.positionData;
                return true;
            }
            System.err.println("Error getting device info: " + rsp.code());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    protected boolean setState(JsonObj state) {
        if (address == null || authKey == null) {
            return false;
        }
        Request req = new Request.Builder()
                .url(String.format("http://%s/api/v1/%s/state", address, authKey))
                .addHeader("User-Agent", "Nanoleaf Sacn")
                .put(RequestBody.create(state.toStringC(), MIME_APPLICATION_JSON))
                .build();
        try (Response rsp = httpClient.newCall(req).execute()) {
            if (rsp.code() == 204) {
                return true;
            }
            System.err.println("Error setting device state: " + rsp.code());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    
    protected JsonObj getState(String param) {
        if (address == null || authKey == null) {
            return null;
        }
        Request req = new Request.Builder()
                .url(String.format("http://%s/api/v1/%s/state/%s", address, authKey, param))
                .addHeader("User-Agent", "Nanoleaf Sacn")
                .get()
                .build();
        try (Response rsp = httpClient.newCall(req).execute()) {
            if (rsp.code() == 200) {
                return JsonObj.parseD(rsp.body().string());
            }
            System.err.println("Error getting device state: " + rsp.code());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public boolean setEffect(JsonObj effect) {
        if (address == null || authKey == null) {
            return false;
        }
        JsonObj body = JsonObj.ofObject();
        body.setKey("write", effect);
        Request req = new Request.Builder()
                .url(String.format("http://%s/api/v1/%s/effects", address, authKey))
                .addHeader("User-Agent", "Nanoleaf Sacn")
                .put(RequestBody.create(body.toStringC(), MIME_APPLICATION_JSON))
                .build();
        try (Response rsp = httpClient.newCall(req).execute()) {
            if (rsp.code() == 204) {
                return true;
            }
            System.err.println("Error setting device state: " + rsp.code());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean identify() {
        if (address == null || authKey == null) {
            return false;
        }
        Request req = new Request.Builder()
                .url(String.format("http://%s/api/v1/%s/identify", address, authKey))
                .addHeader("User-Agent", "Nanoleaf Sacn")
                .put(RequestBody.create("", MediaType.get("text/plain")))
                .build();
        try (Response rsp = httpClient.newCall(req).execute()) {
            if (rsp.code() == 200) {
                return true;
            }
            System.err.println("Error identifying device: " + rsp.code());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setStreaming() {
        if (address == null || authKey == null) {
            return false;
        }
        Request req = new Request.Builder()
                .url(String.format("http://%s/api/v1/%s/effects", address, authKey))
                .addHeader("User-Agent", "Nanoleaf Sacn")
                .put(RequestBody.create(
                        "{\"command\":\"display\",\"animType\":\"extControl\",\"extControlVersion\":\"v2\"}",
                        MIME_APPLICATION_JSON))
                .build();
        try (Response rsp = httpClient.newCall(req).execute()) {
            if (rsp.code() == 200) {
                return true;
            }
            System.err.println("Error setting device to streaming: " + rsp.code());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public ColorState buildColorState() {
        return new ColorState(this);
    }

    public void updateDmx() {
        if(dmxAddress == null)
            return;
        // Main.LOGGER.debug("Updating dmx for device {} at address {}", deviceName, dmxAddress);
        SacnUniverse uni = Main.sacnListener.getUniverse(dmxAddress.universe);
        ColorState state = buildColorState();
        int nextAddress = dmxAddress.start - 1;
        for (int i = 0; i < order.length; i++) {
            // Main.LOGGER.debug("- {} ~ {} {} {}", nextAddress, uni.data[nextAddress], uni.data[nextAddress + 1], uni.data[nextAddress + 2]);
            state.setColor(order[i],
                    new Color(uni.data[nextAddress], uni.data[nextAddress + 1], uni.data[nextAddress + 2]));
            nextAddress += 3;
        }
        setEffect(state.build(1));
    }
}
