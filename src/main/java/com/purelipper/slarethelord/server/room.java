package com.purelipper.slarethelord.server;

import com.google.gson.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class room {
    private String roomName;
    private InetAddress serverIP;
    private boolean ifPwdRequired = false;
    private String roomPassword;
    private int owner = 0;
    private final player[] players = new player[4];
    private boolean[] isReady = new boolean[4];
    private int status = WAITING;

    private static final int WAITING = 0;
    private static final int PLAYING = 1;

    public static room parseRoomFromJson(String json) throws JsonParseException, UnknownHostException {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        if (!obj.has("roomName") ||
                !obj.has("serverIP") ||
                !obj.has("ifPwdRequired") ||
                !obj.has("roomPassword") ||
                !obj.has("players") ||
                !obj.has("isReady") ||
                !obj.has("status")) {
            throw new JsonParseException("Invalid JSON: member missed");
        }

        String roomName = obj.get("roomName").getAsString();
        String IP = obj.get("serverIP").getAsString();
        boolean ifPwdRequired = obj.get("ifPwdRequired").getAsBoolean();
        String roomPassword = obj.get("roomPassword").getAsString();
        JsonArray players = obj.getAsJsonArray("players");
        boolean[] isReady = new boolean[4];
        int status = obj.get("status").getAsInt();

        InetAddress serverIP = InetAddress.getByName(IP);
        room r = new room(roomName, serverIP, ifPwdRequired, roomPassword, isReady, status);

        int pos = 0;
        if (players != null) {
            for (JsonElement p : players) {
                if (p != null) {
                    r.setPlayer(pos,player.parsePlayerFromJson(new Gson().toJson(p)));
                }
                pos++;
            }
        }
        return r;
    }

    public room(String roomName, InetAddress serverIP, boolean ifPwdRequired, String roomPassword, boolean[] isReady, int status) {
        this.roomName = roomName;
        this.serverIP = serverIP;
        this.ifPwdRequired = ifPwdRequired;
        this.roomPassword = roomPassword;
        this.isReady = isReady;
        this.status = status;
    }

    public room(String roomName, InetAddress serverIP, boolean ifPwdRequired, String roomPassword) {
        this.roomName = roomName;
        this.serverIP = serverIP;
        this.ifPwdRequired = ifPwdRequired;
        this.roomPassword = roomPassword;
        this.isReady = new boolean[]{false, false, false, false};
        this.status = WAITING;
    }

    public room(String roomName, InetAddress serverIP, boolean ifPwdRequired) {
        this.roomName = roomName;
        this.serverIP = serverIP;
        this.ifPwdRequired = ifPwdRequired;
        this.roomPassword = "";
        this.isReady = new boolean[]{false, false, false, false};
        this.status = WAITING;
    }

    public void setPlayer(int pos, player p) {
        players[pos] = p;
    }

    public void join(player p, int position) {
        if (isRoomEmpty()) {
            owner = position;
            p.setOwner(true);
        }
        players[position] = p;
    }

    public void quit(int position) {
        if (players[position] != null) {
            players[position] = null;
            isReady[position] = false;
            if (position == owner) {
                int newOwner = findNextOwner();
                owner = newOwner;
                if (newOwner != -1) {
                    players[newOwner].setOwner(true);
                }
            }
        }
    }

    private int findNextOwner() {
        for (int i = 0; i < players.length; i++) {
            if (players[i] != null) {
                return i;
            }
        }
        return -1;
    }

    public boolean isRoomEmpty() {
        for (player p : players) {
            if (p != null) {
                return false;
            }
        }
        return true;
    }

    public String getRoomName() {
        return roomName;
    }

    public boolean isIfPwdRequired() {
        return ifPwdRequired;
    }

    public String getRoomPassword() {
        return roomPassword;
    }

    public player[] getPlayers() {
        return players;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setIfPwdRequired(boolean ifPwdRequired) {
        this.ifPwdRequired = ifPwdRequired;
    }

    public void setRoomPassword(String roomPassword) {
        this.roomPassword = roomPassword;
    }

    public void getReady(int position) {
        isReady[position] = true;
    }

    public boolean isAllReady() {
        for (int i = 0; i < players.length; i++) {
            if (players[i] != null && !isReady[i]) {
                return false;
            }
        }
        return true;
    }


    public InetAddress getServerIP() {
        return serverIP;
    }

    public void setServerIP(InetAddress serverIP) {
        this.serverIP = serverIP;
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "room{" +
                "roomName='" + roomName + '\'' +
                ", serverIP=" + serverIP +
                ", ifPwdRequired=" + ifPwdRequired +
                ", roomPassword='" + roomPassword + '\'' +
                ", owner=" + owner +
                ", players=" + Arrays.toString(players) +
                ", isReady=" + Arrays.toString(isReady) +
                ", status=" + status +
                '}';
    }
}
