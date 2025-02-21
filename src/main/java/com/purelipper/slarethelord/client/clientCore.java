package com.purelipper.slarethelord.client;

import com.google.gson.Gson;
import com.purelipper.slarethelord.message.message;
import com.purelipper.slarethelord.message.messageConsumer;
import com.purelipper.slarethelord.message.messageListener;
import com.purelipper.slarethelord.server.cardStack;
import com.purelipper.slarethelord.server.player;
import com.purelipper.slarethelord.server.room;
import com.purelipper.slarethelord.server.serverCore;
import com.purelipper.slarethelord.util.NetworkUtils;

import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class clientCore {
    private player p;
    private static final int SERVER_PORT = 16000;
    private static final int CLIENT_PORT = 16001;

    private final messageListener listener;
    private final messageConsumer consumer;

    private final ArrayList<room> rooms = new ArrayList<>();
    private room currentRoom;

    public clientCore() {
        try {
            p = new player("", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        listener = new messageListener(CLIENT_PORT);
        consumer = new messageConsumer(listener) {
            @Override
            public void dealMessage(message msg) {
                switch (msg.getLabel()) {
                    case "roomOffer" -> {
                        try {
                            rooms.add(room.parseRoomFromJson(msg.getContent()));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    case "joinSucceed" -> {
                        try {
                            currentRoom = room.parseRoomFromJson(msg.getContent());
                            System.out.println("join room \"" + currentRoom.getRoomName() + "\" succeed: ");
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    case "onlineDetect" -> {
//                        System.out.println("onlineDetect received");
                        try {
                            NetworkUtils.sendMessage(new message("onlineDetectAck", InetAddress.getLocalHost(), CLIENT_PORT,
                                    msg.getSourceIP(), msg.getSourcePort(), ""));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    case "quitSucceed" -> {
                        System.out.println("quit room \"" + currentRoom.getRoomName() + "\" succeed");
                        currentRoom = null;
                    }
                    case "syncRoom" -> {
                        try {
                            currentRoom = room.parseRoomFromJson(msg.getContent());
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    case "becomeHost" -> {
                        System.out.println("I am now the new host! Starting server...");
                        serverCore newServer = new serverCore();
                        newServer.openListener();
                        newServer.openConsumer();
                        newServer.createRoom(currentRoom.getRoomName(), currentRoom.isIfPwdRequired(), currentRoom.getRoomPassword());
                    }
                    case "newHost" -> {
                        try {
                            currentRoom.setServerIP(InetAddress.getByName(msg.getContent()));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    case "gameStartNotice" -> {
                        //TODO:进入游戏界面

                    }
                    case "initialCards" -> {
                        p.getHandCards().copyStack(cardStack.parseStackFromJson(msg.getContent()));
//                        try {
//                            NetworkUtils.sendMessage(new message("cardsReceived", InetAddress.getLocalHost(), CLIENT_PORT,
//                                    msg.getSourceIP(), msg.getSourcePort(), ""));
//                        } catch (UnknownHostException e) {
//                            throw new RuntimeException(e);
//                        }
                    }
                }
            }
        };
    }

    private void askForRoom() {
        try {
            NetworkUtils.sendMessage(new message("roomAsk", InetAddress.getLocalHost(), CLIENT_PORT, NetworkUtils.getBroadcastAddress(), SERVER_PORT, ""));
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
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

    public void launch() {
        openListener();
        openConsumer();
        Scanner scanner = new Scanner(System.in);
        System.out.println("please enter your name:");
        p.setName(scanner.nextLine());
        while (true) {
            if (p.isOwner()) {
                System.out.println("you want to " +
                        "1.ask for rooms; " +
                        "2.join room; " +
                        "3.change your name; " +
                        "4.show rooms; " +
                        "5.quit room; " +
                        "6.get ready; " +
                        "7.create room; " +
                        "8.start game; ");
            } else {
                System.out.println("you want to " +
                        "1.ask for rooms; " +
                        "2.join room; " +
                        "3.change your name; " +
                        "4.show rooms; " +
                        "5.quit room; " +
                        "6.get ready; " +
                        "7.create room; ");
            }
            int input = scanner.nextInt();
            scanner.nextLine();
            switch (input) {
                case 1 -> {
                    rooms.clear();
                    askForRoom();
                }
                case 2 -> {
                    System.out.println("rooms:");
                    System.out.println(rooms.toString());
                    System.out.println("which one?(from 0)");
                    int roomId = scanner.nextInt();
                    scanner.nextLine();
                    System.out.println("joining...");
                    try {
                        NetworkUtils.sendMessage(new message("joinRoom", InetAddress.getLocalHost(), CLIENT_PORT,
                                rooms.get(roomId).getServerIP(), SERVER_PORT, new Gson().toJson(p)));
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
                case 3 -> {
                    System.out.println("please enter your name:");
                    p.setName(scanner.nextLine());
                }
                case 4 -> {
                    System.out.println("rooms:");
                    System.out.println(rooms.toString());
                }
                case 5 -> {
                    if (currentRoom != null) {
                        System.out.println("quiting...");
                        try {
                            NetworkUtils.sendMessage(new message("quitRoom", InetAddress.getLocalHost(), CLIENT_PORT, currentRoom.getServerIP(), SERVER_PORT, ""));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        System.out.println("you are not in any room");
                    }
                }
                case 6 -> {
                    if (currentRoom != null) {
                        try {
                            NetworkUtils.sendMessage(new message("getReady", InetAddress.getLocalHost(), CLIENT_PORT, currentRoom.getServerIP(), SERVER_PORT, ""));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        System.out.println("you are not in any room");
                    }
                }
                case 7 -> {
                    System.out.println("room name: ");
                    String name = scanner.nextLine();
                    System.out.println("use password?(Y/N)");
                    char pass = scanner.next().charAt(0);
                    scanner.nextLine();
                    String password = "";
                    if (pass == 'Y') {
                        System.out.println("input password: ");
                        password = scanner.nextLine();
                    }
                    serverCore core = new serverCore();
                    core.openListener();
                    core.openConsumer();
                    core.createRoom(name, pass == 'Y', password);
                    try {
                        NetworkUtils.sendMessage(new message("joinRoom", InetAddress.getLocalHost(), CLIENT_PORT,
                                InetAddress.getLocalHost(), SERVER_PORT, new Gson().toJson(p)));
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
                case 8 -> {
                    if (p.isOwner()) {
                        if (currentRoom.isAllReady()) {
                            try {
                                NetworkUtils.sendMessage(new message("gameStart",InetAddress.getLocalHost(),CLIENT_PORT,
                                        currentRoom.getServerIP(), SERVER_PORT, ""));
                            } catch (UnknownHostException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            System.out.println("somebody is not ready yet!");
                        }
                    }
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        clientCore core = new clientCore();
        core.launch();
    }
}
