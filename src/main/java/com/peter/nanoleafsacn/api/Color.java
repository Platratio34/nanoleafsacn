package com.peter.nanoleafsacn.api;

import com.peter.webserver.data.JsonObj;

public class Color {

    public int red;
    public int green;
    public int blue;

    public Color(int brightness) {
        red = brightness;
        green = brightness;
        blue = brightness;
    }

    public Color(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public Color(byte red, byte green, byte blue) {
        this.red = red & 0xff;
        this.green = green & 0xff;
        this.blue = blue & 0xff;
    }
    
    public Color(Color color) {
        red = color.red;
        green = color.green;
        blue = color.blue;
    }

    public Color(JsonObj json) {
        red = json.getInt("red", json.getInt("r", 0));
        green = json.getInt("green", json.getInt("g", 0));
        blue = json.getInt("blue", json.getInt("b", 0));
    }
}
