package com.peter.nanoleafsacn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.HashMap;

import com.peter.nanoleafsacn.api.Device;
import com.peter.nanoleafsacn.http.HTTPMessage;

import okhttp3.OkHttpClient;

public class DiscoveryListener implements Runnable {

    private final Thread thread;

    public boolean running = true;
    
    public final HashMap<String, Device> knownDevices = new HashMap<>();

    public final OkHttpClient httpClient = new OkHttpClient();

    public void stop() {
        running = false;
    }

    public DiscoveryListener() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        String groupIp = "239.255.255.250"; // A valid Class D multicast address
        int port = 1900;

        // 1. Initialize the MulticastSocket on the specified port
        try (MulticastSocket socket = new MulticastSocket(port)) {

            // 2. Identify the network group and interface
            InetSocketAddress groupAddress = new InetSocketAddress(InetAddress.getByName(groupIp), port);

            // Get the default or a specific network interface (e.g., "eth0" or "wlan0")
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

            if (networkInterface == null) {
                // Fallback to searching by a common default interface if getLocalHost() returns null
                networkInterface = NetworkInterface.getByName("eth0");
            }

            // 3. Join the multicast group on the specific interface
            System.out.println("Joining multicast group " + groupIp + " on port " + port + "on interface "
                    + networkInterface.getName() + " ...");
            socket.joinGroup(groupAddress, networkInterface);

            // 4. Create a buffer to receive incoming messages
            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // System.out.println("Waiting for discovery packet");
                socket.receive(packet); // This call blocks until a packet arrives

                // 5. Process the received packet
                String message = new String(packet.getData(), 0, packet.getLength());
                // System.out.println("Received message: `" + message + "`");
                // System.out.println("Received from: " + packet.getAddress().getHostAddress());

                HTTPMessage msg = HTTPMessage.parse(message);
                // System.out.println(msg);
                if (msg.method.equals("NOTIFY")) {
                    if (!msg.hasHeader("NT") || !msg.getHeader("NT").equals("nanoleaf:nl42")) {
                        continue;
                    }
                    String uuid = msg.getHeader("USN").get();
                    switch (msg.getHeader("NTS").get()) {
                        case "ssdp:alive" -> {
                            if (!knownDevices.containsKey(uuid)) {
                                knownDevices.put(uuid, new Device(uuid));
                                System.out.println("Found new device " + uuid);
                            } else {
                                // System.out.println("Updating device " + uuid);
                            }
                            knownDevices.get(uuid).update(msg);
                        }
                        case "ssdp:byebye" -> {
                            if (knownDevices.containsKey(uuid)) {
                                knownDevices.get(uuid).markUnavailable();
                            }
                        }
                    }
                }
            }

            // 6. Explicitly leave the group when done
            socket.leaveGroup(groupAddress, networkInterface);
            System.out.println("Left the multicast group.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
