package com.peter.nanoleafsacn.sacn;

import java.net.DatagramPacket;

import org.apache.logging.log4j.Logger;

import com.peter.nanoleafsacn.Main;

public class SacnPacket {

    public int preSize;
    public int postSize;
    public long cid;
    public String sourceName;
    public byte priority;
    public int universe;
    public byte startCode;
    public byte[] data;

    public final boolean valid;

    private final byte[] buffer;

    public SacnPacket(byte[] buffer, int length, Logger logger) {
        this.buffer = buffer;
        preSize = getUint16(0); 
        postSize = getUint16(2);
        
        long dataType = getUint32(18);
        if (dataType != 0x04) {
            logger.warn("Invalid data type, expected 0x04, got 0x{}", Long.toHexString(dataType));
            valid = false;
            return;
        }

        long vector2 = getUint32(40);
        if (vector2 != 0x02) {
            logger.warn("Invalid vector 2, expected 0x02, got 0x{}", Long.toHexString(vector2));
            valid = false;
            return;
        }

        // CID? don't know what we'll see if it's necessary later...

        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0x2c; i < 0x6b; i++) {
            nameBuilder.append((char) buffer[i]);
        }
        sourceName = nameBuilder.toString().trim();

        priority = buffer[0xc6];
        universe = getUint16(0x71);

        int dataFormat = buffer[0x76] & 0xff;
        if (dataFormat != 0xa1) {
            logger.warn("Invalid data format, expected 0xa1, got 0x{}", Integer.toHexString(dataFormat));
            valid = false;
            return;
        }

        startCode = buffer[0x7d];

        int dmxLength = getUint16(0x7b) - 1;
        data = new byte[dmxLength];
        // String out = "";
        for (int i = 0; i < dmxLength; i++) {
            data[i] = buffer[i + 0x7e];
            // out += String.format("%02x ", data[i] & 0xff);
        }
        // if(startCode == 0x00)
            // Main.LOGGER.debug(out);
        valid = true;
    }

    private int getUint16(int start) {
        return (buffer[start] << 8) | (buffer[start + 1] & 0xff);
    }
    private long getUint32(int start) {
        return (buffer[start] << 24) | (buffer[start + 1] << 16) | (buffer[start + 2] << 8) | (buffer[start + 3] & 0xff);
    }
}
