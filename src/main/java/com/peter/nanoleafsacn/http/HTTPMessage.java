package com.peter.nanoleafsacn.http;

import java.util.HashMap;

public class HTTPMessage {

    public String method;
    public String uri;
    public String version;

    private final HashMap<String, HTTPHeader> headers = new HashMap<>();

    public String body = null;

    @Override
    public String toString() {
        String out = String.format("HTTPMessage{method=\"%s\",uri=\"%s\",version=\"%s\",headers=[", method, uri,
                version);
        boolean f = true;
        for (HTTPHeader header : headers.values()) {
            if (!f)
                out += ",";
            out += header;
            f = false;
        }
        out += "]";
        if(body != null && !body.isBlank()) {
            out += String.format(",body=\"%s\"", body);
        }
        return out += "]}";
    }

    public void addHeader(String name, String value) {
        name = name.toUpperCase();
        if (headers.containsKey(name)) {
            headers.get(name).add(value);
        } else {
            headers.put(name, new HTTPHeader(name, value));
        }
    }

    public boolean hasHeader(String name) {
        return headers.containsKey(name.toUpperCase());
    }
    
    public HTTPHeader getHeader(String name) {
        return headers.getOrDefault(name.toUpperCase(), null);
    }

    public static class Parser {
        private HTTPMessage msg;
        private String message;
        private int i = 0;

        public Parser(String message) {
            msg = new HTTPMessage();
            this.message = message;

            msg.method = parseWord();
            msg.uri = parseWord();
            msg.version = parseWord();

            while (parseHeaders()) {

            }
            if(message.length() > i)
                msg.body = message.substring(i);
        }

        private String parseWord() {
            String out = "";
            while (i < message.length()) {
                char c = message.charAt(i++);
                if (Character.isWhitespace(c)) {
                    if (out.isBlank())
                        continue;
                    if (c == '\r') {
                        i++;
                    }
                    break;
                }
                out += c;
            }
            return out;
        }

        private boolean parseHeaders() {
            String name = "";
            String value = null;
            if (i >= message.length()) {
                return false;
            } else if (message.charAt(i) == '\r') {
                i += 3;
                return false;
            }
            while (i < message.length()) {
                char c = message.charAt(i++);
                // System.out.println(c);
                if (value == null && c == ':') {
                    value = "";
                    continue;
                } else if (Character.isWhitespace(c)) {
                    if (name.isBlank() || (value != null && value.isBlank()))
                        continue;
                    if (c == '\r') {
                        i++;
                        break;
                    }
                }

                if (value == null) {
                    name += c;
                } else {
                    value += c;
                }
            }
            if (name.isBlank()) {
                throw new IllegalStateException("Name must be non-blank");
            }
            // System.out.println(String.format("`%s`: `%s`", name, value));
            msg.addHeader(name, value);
            return true;
        }
    }

    public static HTTPMessage parse(String message) {
        return new Parser(message).msg;
    }
}
