package com.purelipper.slarethelord.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.lang.reflect.Array;

public class player {
    private String name;
    private String playerIP;
    private int connectionStatus;
    private boolean isOwner;
    private final cardStack handCards = new cardStack("handCards");

    public player(String name, String playerIP, boolean isOwner, int connectionStatus) {
        this.name = name;
        this.playerIP = playerIP;
        this.isOwner = isOwner;
        this.connectionStatus = connectionStatus;
    }

    public player(String name, String playerIP) {
        this.name = name;
        this.playerIP = playerIP;
        this.isOwner = false;
        this.connectionStatus = 3;
    }

    public static player parsePlayerFromJson(String json) throws JsonParseException {
        if (json == null || json.equals("null")) {
            return null; // 避免解析错误
        }
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("playerIP") ||
                !obj.has("name") ||
                !obj.has("connectionStatus") ||
                !obj.has("isOwner")) {
            throw new JsonParseException("playerIP is missing or invalid");
        }
        String playerIP = obj.get("playerIP").getAsString();
        String name = obj.get("name").getAsString();
        int connectionStatus = obj.get("connectionStatus").getAsInt();
        boolean isOwner = obj.get("isOwner").getAsBoolean();
        return new player(name, playerIP, isOwner, connectionStatus);
    }

    public String getPlayerIP() {
        return playerIP;
    }

    public cardStack getHandCards() {
        return handCards;
    }

    public void setPlayerIP(String playerIP) {
        this.playerIP = playerIP;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    /**
     * 玩家网络状况变好
     */
    public void connectionStatusUp() {
        connectionStatus++;
    }

    /**
     * 玩家网络状况变差
     */
    public void connectionStatusDown() {
        connectionStatus--;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean isOwner) {
        this.isOwner = isOwner;
    }

    @Override
    public String toString() {
        return "player{" +
                "name='" + name + '\'' +
                ", playerIP='" + playerIP + '\'' +
                ", connectionStatus=" + connectionStatus +
                ", isOwner=" + isOwner +
                ", handCards=" + handCards +
                '}';
    }

    public void showCards(){
        System.out.println(handCards);
    }
}
