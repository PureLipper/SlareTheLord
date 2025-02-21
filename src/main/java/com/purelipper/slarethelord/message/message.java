package com.purelipper.slarethelord.message;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.InetAddress;

public class message {
    private final String label;
    private final InetAddress sourceIP;
    private final int sourcePort;
    private final InetAddress destinationIP;
    private final int destinationPort;
    private final String content;

    public static message parseMessageFromJson(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            if (!obj.has("label") ||
                    !obj.has("sourceIP") ||
                    !obj.has("sourcePort") ||
                    !obj.has("content") ||
                    !obj.has("destinationIP") ||
                    !obj.has("destinationPort")) {
                return null;
            }

            String label = obj.get("label").getAsString();
            String sIP = obj.get("sourceIP").getAsString();
            int sourcePort = obj.get("sourcePort").getAsInt();
            String content = obj.get("content").getAsString();
            String dIP = obj.get("destinationIP").getAsString();
            int destinationPort = obj.get("destinationPort").getAsInt();

            InetAddress sourceIP = InetAddress.getByName(sIP);
            InetAddress destinationIP = InetAddress.getByName(dIP);

            return new message(label, sourceIP, sourcePort, destinationIP, destinationPort, content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public message(String label, InetAddress sourceIP, int sourcePort, InetAddress destinationIP, int destinationPort, String content) {
        this.label = label;
        this.sourceIP = sourceIP;
        this.sourcePort = sourcePort;
        this.destinationIP = destinationIP;
        this.destinationPort = destinationPort;
        this.content = content;
    }

    public InetAddress getSourceIP() {
        return sourceIP;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public String getContent() {
        return content;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "message{" +
                "label='" + label + '\'' +
                ", sourceIP=" + sourceIP +
                ", sourcePort=" + sourcePort +
                ", content='" + content + '\'' +
                '}';
    }

    public InetAddress getDestinationIP() {
        return destinationIP;
    }

    public int getDestinationPort() {
        return destinationPort;
    }
}