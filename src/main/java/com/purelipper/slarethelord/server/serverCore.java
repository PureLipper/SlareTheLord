package com.purelipper.slarethelord.server;

import com.google.gson.Gson;
import com.purelipper.slarethelord.message.message;
import com.purelipper.slarethelord.message.messageConsumer;
import com.purelipper.slarethelord.message.messageListener;
import com.purelipper.slarethelord.util.NetworkUtils;

import java.net.*;

public class serverCore {
    public static final int SERVER_PORT = 16000; // 服务器监听端口
    public static final int CLIENT_PORT = 16001; // 广播目标端口

    private final messageListener listener;
    private final messageConsumer consumer;
    private final Thread game;
    private gameCore gameCore;

    private room r;

    public serverCore() {
        listener = new messageListener(SERVER_PORT);
        consumer = new messageConsumer(listener) {
            @Override
            public void dealMessage(message msg) {
                switch (msg.getLabel()) {
                    case "roomAsk" -> {
                        System.out.println("room ask received");
                        try {
                            NetworkUtils.sendMessage(new message("roomOffer", InetAddress.getLocalHost(), SERVER_PORT,
                                    msg.getSourceIP(), CLIENT_PORT, new Gson().toJson(r)));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    case "joinRoom" -> {
                        player[] players = r.getPlayers();
                        for (int i = 0; i < players.length; i++) {
                            if (players[i] == null) {
                                r.join(player.parsePlayerFromJson(msg.getContent()), i);
                                try {
                                    NetworkUtils.sendMessage(new message("joinSucceed", InetAddress.getLocalHost(), SERVER_PORT,
                                            msg.getSourceIP(), CLIENT_PORT, new Gson().toJson(r)));
                                } catch (UnknownHostException e) {
                                    throw new RuntimeException(e);
                                }
                                System.out.println("player \"" + players[i].getName() + "\" joined room \"" + r.getRoomName() + '"');
                                break;
                            }
                        }
                        try {
                            NetworkUtils.sendMessage(new message("joinFailed", InetAddress.getLocalHost(), SERVER_PORT,
                                    msg.getSourceIP(), CLIENT_PORT, "the room is full"));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                        syncRoom();
                    }
                    case "onlineDetectAck" -> {
                        System.out.println("onlineDetectAck received from: " + msg.getSourceIP());
                        for (player player : r.getPlayers()) {
                            try {
                                if (player != null && player.getPlayerIP().equals(msg.getSourceIP().getHostAddress())) {
                                    player.connectionStatusUp();
                                    break;
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    case "quitRoom" -> {
                        player[] players = r.getPlayers();
                        int leavingPlayerIndex = -1;

                        for (int i = 0; i < players.length; i++) {
                            if (players[i] != null && players[i].getPlayerIP().equals(msg.getSourceIP().getHostAddress())) {
                                leavingPlayerIndex = i;
                                System.out.println("Player \"" + players[i].getName() + "\" quit room \"" + r.getRoomName() + "\"");
                                r.quit(i);
                                break;
                            }
                        }

                        if (leavingPlayerIndex == -1) {
                            System.out.println("Quit request from unknown player, ignoring...");
                            return;
                        }

                        // 如果房主退出，寻找新的房主
                        if (r.getOwner() == leavingPlayerIndex) {
                            handleOwnerQuit();
                        }

                        try {
                            NetworkUtils.sendMessage(new message("quitSucceed", InetAddress.getLocalHost(), SERVER_PORT,
                                    msg.getSourceIP(), CLIENT_PORT, new Gson().toJson(r)));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    case "getReady" -> {
                        player[] players = r.getPlayers();
                        for (int i = 0; i < players.length; i++) {
                            if (players[i].getPlayerIP().equals(msg.getSourceIP().getHostAddress())) {
                                System.out.println("player \"" + players[i].getName() + "\" is ready");
                                r.getReady(i);
                                break;
                            }
                        }
                    }

                    case "gameStart" -> {
                        player[] players = r.getPlayers();
                        for (player player : players) {
                            if (player != null) {
                                try {
                                    NetworkUtils.sendMessage(new message("gameStartNotice", InetAddress.getLocalHost(), SERVER_PORT,
                                            InetAddress.getByName(player.getPlayerIP()), CLIENT_PORT, ""));
                                } catch (UnknownHostException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        game.start();
                    }

                    default -> gameCore.dealMessage(msg);
                }
            }
        };

        game = new Thread(() -> {
            gameCore = new gameCore(r.getPlayers());
            gameCore.launch();
        });
    }

    public void createRoom(String roomName, boolean ifPwdRequired, String roomPassword) {
        try {
            r = new room(roomName, InetAddress.getLocalHost(), ifPwdRequired, roomPassword);
            Thread onlineDetector = new Thread(() -> {
                player[] players = r.getPlayers();
                while (true) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    for (int i = 0; i < players.length; i++) {
                        if (players[i] != null) {
                            try {
                                NetworkUtils.sendMessage(new message("onlineDetect", InetAddress.getLocalHost(), SERVER_PORT,
                                        InetAddress.getByName(players[i].getPlayerIP()), CLIENT_PORT, ""));
                                players[i].connectionStatusDown();
                                if (players[i].getConnectionStatus() <= -1) {
                                    System.out.println("player " + players[i].getName() + " lost connection");
                                    players[i] = null;
                                }
                            } catch (UnknownHostException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    //额外检查房间是否为空
                    if (r.isRoomEmpty()) {
                        System.out.println("Room \"" + r.getRoomName() + "\" is now empty. Server will shut down.");
                        shutdownServer();
                        return; // 退出线程
                    }
                }
            });
            onlineDetector.start();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdownServer() {
        closeListener();
        closeConsumer();
        System.out.println("Server has stopped");
    }

    public void noticeRoom(room r) {
        try {
            NetworkUtils.sendMessage(new message("roomOffer", InetAddress.getLocalHost(), SERVER_PORT,
                    NetworkUtils.getBroadcastAddress(), CLIENT_PORT, new Gson().toJson(r)));
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void syncRoom() {
        try {
            for (player player : r.getPlayers()) {
                if (player != null) {
                    NetworkUtils.sendMessage(new message("syncRoom", InetAddress.getLocalHost(), SERVER_PORT,
                            InetAddress.getByName(player.getPlayerIP()), CLIENT_PORT, new Gson().toJson(r)));
                }
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleOwnerQuit() {
        player[] players = r.getPlayers();
        int newOwnerIndex = -1;

        // 查找新的房主
        for (int i = 0; i < players.length; i++) {
            if (players[i] != null) {
                newOwnerIndex = i;
                break;
            }
        }

        if (newOwnerIndex != -1) {
            r.setOwner(newOwnerIndex);  // 更新房主座位号
            player newOwner = players[newOwnerIndex];
            System.out.println("New owner found: " + newOwner.getName() + ", transferring server...");

            // 通知新房主成为服务器
            try {
                NetworkUtils.sendMessage(new message("becomeHost", InetAddress.getLocalHost(), SERVER_PORT,
                        InetAddress.getByName(newOwner.getPlayerIP()), CLIENT_PORT, new Gson().toJson(r)));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            // 通知所有玩家新的服务器 IP
            for (player p : players) {
                if (p != null && !p.equals(newOwner)) {
                    try {
                        NetworkUtils.sendMessage(new message("newHost", InetAddress.getLocalHost(), SERVER_PORT,
                                InetAddress.getByName(p.getPlayerIP()), CLIENT_PORT, newOwner.getPlayerIP()));
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            // 关闭当前服务器
            shutdownServer();
        } else {
            System.out.println("Room is empty. Server shutting down...");
            shutdownServer();
        }
    }


    public void openListener() {
        listener.start();
    }

    public void closeListener() {
        listener.stop();
    }

    public void openConsumer() {
        consumer.start();
    }

    public void closeConsumer() {
        consumer.stop();
    }

    public static void main(String[] args) {
        serverCore core = new serverCore();
        core.openListener();
        core.openConsumer();
        core.createRoom("myRoom", false, "");

    }
}
