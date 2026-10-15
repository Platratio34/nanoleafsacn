package com.peter.nanoleafsacn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.peter.nanoleafsacn.api.Device;
import com.peter.nanoleafsacn.sacn.SacnListener;
import com.peter.nanoleafsacn.web.DeviceAPI;
import com.peter.nanoleafsacn.web.DeviceViewPage;
import com.peter.nanoleafsacn.web.DevicesPage;
import com.peter.nanoleafsacn.web.SacnAPI;
import com.peter.webserver.data.JsonObj;
import com.peter.webserver.tui.SimpleCommand;
import com.peter.webserver.tui.TUI;
import com.peter.webserver.web.Responses;
import com.peter.webserver.web.URLPath;
import com.peter.webserver.web.WebPage;
import com.peter.webserver.web.WebServer;

public class Main {

    public static final TUI tui = new TUI();

    private static String cachedBanner;
    private static long bannerCacheTime = -1;
    public static WebServer webserver;

    public static DiscoveryListener discoveryListener;

    public static final HashMap<String, Device> activeDevices = new HashMap<>();

    public static final SacnListener sacnListener = new SacnListener("SacnListener");

    public static Path savePath;

    public static final Logger LOGGER = LogManager.getLogger("NanoleafSACN");

    public static final HashSet<String> flags = new HashSet<>();
    public static final HashMap<String, String> configVals = new HashMap<>();

    public static final UpdateThread updateThread = new UpdateThread();

    public static void main(String[] args) {
        for (String a : args) {
            if (a.startsWith("-")) {
                if (a.contains("=")) {
                    int i = a.indexOf("=", 0);
                    String name = a.substring(1, i);
                    String value = a.substring(i + 1);
                    LOGGER.info("`{}`: `{}`", name, value);
                    configVals.put(name, value);
                } else {
                    flags.add(a.substring(1));
                }
            }
        }

        int port = 8080;
        if (configVals.containsKey("p")) {
            try {
                port = Integer.parseInt(configVals.get("p"));
            } catch (NumberFormatException e) {

            }
        }
        webserver = new WebServer(port, configVals.getOrDefault("root", ""));
        savePath = webserver.rootPath.resolve(configVals.getOrDefault("path", "data/save.json"));
        LOGGER.info("Save path: {}", savePath);

        Responses.bannerProvider = (request) -> {
            long t = System.currentTimeMillis();
            if (bannerCacheTime <= t) {
                try {
                    cachedBanner = webserver.getFile("html/banner.html");
                    bannerCacheTime = t + (1000/* * 60 * 5*/);
                } catch (FileNotFoundException e) {
                    cachedBanner = "";
                    webserver.logger.error("Error loading banner html", e);
                }
            }
            return cachedBanner;
        };

        webserver.addEndpoint(new WebPage(URLPath.of(""), "index.html"));


        discoveryListener = new DiscoveryListener();
        if (savePath.toFile().exists()) {
            JsonObj save = JsonObj.parseP(savePath);
            if (save == null) {
                System.err.println("Corrupted save");
            }
            JsonObj devices = save.getKey("devices");
            for (JsonObj jD : devices.getArr()) {
                Device d = Device.fromJson(jD);
                discoveryListener.knownDevices.put(d.uuid, d);
                activeDevices.put(d.uuid, d);
                if(d.dmxAddress != null)
                    sacnListener.listenTo(d.dmxAddress.universe);
            }
        }
        webserver.addEndpoint(new DevicesPage());
        webserver.addEndpoint(new DeviceViewPage());
        webserver.addEndpoint(new DeviceAPI());

        sacnListener.start();
        webserver.addEndpoint(new SacnAPI());
        webserver.addEndpoint(new WebPage(URLPath.of("sacn"), "sacn.html"));

        updateThread.start();

        if (!flags.contains("nt")) {
            tui.addCommand(new SimpleCommand("stop", cmd -> {
                webserver.logger.info("Stopping . . .");
                discoveryListener.stop();
                updateThread.stop();
                System.exit(0);
            }).setDescription("Stops the server"));
            tui.addCommand(sacnListener.getCommand());

            tui.run();
        }
    }
    
    public static void addActiveDevice(Device d) {
        d.active = true;
        activeDevices.put(d.uuid, d);
        saveDevices();
    }
    
    public static void removeActiveDevice(Device d) {
        d.active = false;
        activeDevices.remove(d.uuid);
        saveDevices();
    }

    public static void saveDevices() {
        JsonObj save = JsonObj.ofObject();
        JsonObj devices = JsonObj.ofArray();
        save.setKey("devices", devices);
        for (Device d : activeDevices.values()) {
            devices.addArray(d.toJsonSave());
            if(d.dmxAddress != null)
                sacnListener.listenTo(d.dmxAddress.universe);
        }
        try {
            Files.writeString(savePath, save.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}