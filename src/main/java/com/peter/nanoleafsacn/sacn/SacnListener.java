package com.peter.nanoleafsacn.sacn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.peter.webserver.tui.SimpleCommand;

public class SacnListener {

    public final String name;

    public final HashMap<String, SacnSource> sources = new HashMap<>();

    public final HashMap<Integer, UniverseListener> listeners = new HashMap<>();

    protected final Logger logger;

    public SacnListener(String name) {
        this.name = name;
        logger = LogManager.getLogger(name);
        logger.info("Test");
    }

    public void start() {
        if (running)
            return;
        running = true;
        for (UniverseListener listener : listeners.values()) {
            listener.start();
        }
    }

    private boolean running = false;

    public void stop() {
        running = false;
    }
    
    public void listenTo(int universe) {
        if (listeners.containsKey(universe)) {
            // logger.info("Universe {} already added", universe);
            return;
        }
        UniverseListener listener = new UniverseListener(universe, name);
        listeners.put(universe, listener);
        logger.info("Adding universe {}", universe);
        if (running) {
            listener.start();
        }
    }
    
    public SacnUniverse getUniverse(int universe) {
        SacnUniverse uni = new SacnUniverse(universe, System.currentTimeMillis());
        for (SacnSource source : sources.values()) {
            SacnUniverse u2 = source.get(universe);
            if (u2 != null)
                uni.add(u2);
        }
        return uni;
    }

    public SimpleCommand getCommand() {
        return new SimpleCommand("sacn", cmd -> {
            System.out.println("SACN State");
            if (sources.size() == 0) {
                System.out.println("No sources");
            } else {
                System.out.println("Sources");
                for (SacnSource src : sources.values()) {
                    System.out.println("- " + src.name);
                }
            }
            if (listeners.isEmpty()) {
                System.out.println("No Universes");
            } else {
                System.out.println("Active universes");
                for (Integer i : listeners.keySet()) {
                    System.out.println("- "+i);
                }
            }
        });
    }
    
    private class UniverseListener implements Runnable {

        public static final int PORT = 5568;
        public static final String HOSTNAME = "239.255.";

        protected final Thread thread;
        public final int universe;

        public UniverseListener(int universe, String name) {
            thread = new Thread(this, String.format("%s-%d", name, universe));
            this.universe = universe;
        }

        @Override
        public void run() {
            logger.info("Starting thread for universe {}", universe);
            try (MulticastSocket socket = new MulticastSocket(PORT)) {

                String groupIp = HOSTNAME + (universe >> 8) + "." + (universe & 0xff);
                InetSocketAddress groupAddress = new InetSocketAddress(InetAddress.getByName(groupIp), PORT);

                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

                if (networkInterface == null) {
                    networkInterface = NetworkInterface.getByName("eth0");
                }

                logger.info("Joining multicast group {} on port {} on interface {} ...", groupIp, PORT,
                        networkInterface.getName());
                socket.setReuseAddress(true);
                socket.joinGroup(groupAddress, networkInterface);

                byte[] buffer = new byte[1024];

                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    // logger.debug("Got packet");

                    SacnPacket sacnPacket = new SacnPacket(buffer, packet.getLength(), logger);
                    if (!sacnPacket.valid) {
                        logger.debug("Invalid packet type");
                        continue;
                    }
                    // logger.debug("- Was SACN");
                    if (!sources.containsKey(sacnPacket.sourceName)) {
                        logger.info("New sACN source `{}`", sacnPacket.sourceName);
                        sources.put(sacnPacket.sourceName, new SacnSource(sacnPacket.sourceName));
                    }
                    sources.get(sacnPacket.sourceName).update(sacnPacket);
                }
                socket.leaveGroup(groupAddress, networkInterface);
                logger.info("Leaving multicast group {} on port {} on interface {} ...", groupIp, PORT,
                        networkInterface.getName());

            } catch (IOException e) {
                e.printStackTrace();
            }
        
            while (running) {
                try {

                } catch (Exception e) {

                }
            }
        }

        public void start() {
            thread.start();
        }

    }
}
