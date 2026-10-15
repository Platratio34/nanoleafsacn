package com.peter.nanoleafsacn;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.peter.nanoleafsacn.api.Device;

public class UpdateThread implements Runnable {

    private final Thread thread;
    private boolean running = false;

    public UpdateThread() {
        thread = new Thread(this, "Update Thread");
    }
    
    public void start() {
        if (!running)
            running = true;
        thread.start();
    }
    
    public void stop() {
        running = false;
    }

    public ArrayList<Consumer<Integer>> listeners = new ArrayList<>();

    private int uI = 0;
    @Override
    public void run() {
        while (running) {
            long start = System.currentTimeMillis();
            for (Device d : Main.activeDevices.values()) {
                d.updateDmx();
            }
            for(Consumer<Integer> listener : listeners) {
                listener.accept(uI % 10);
            }
            uI++;
            long sStime = 100 - (System.currentTimeMillis() - start);
            if (sStime > 0) {
                try {
                    Thread.sleep(sStime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
