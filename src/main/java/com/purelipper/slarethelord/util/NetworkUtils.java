package com.purelipper.slarethelord.util;

import com.google.gson.Gson;
import com.purelipper.slarethelord.message.message;
import com.purelipper.slarethelord.server.player;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

import static com.purelipper.slarethelord.server.serverCore.CLIENT_PORT;
import static com.purelipper.slarethelord.server.serverCore.SERVER_PORT;

public class NetworkUtils {
    public static InetAddress getBroadcastAddress() throws SocketException, UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);

        if (networkInterface == null) {
            throw new SocketException("无法获取网络接口");
        }

        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
            InetAddress broadcast = interfaceAddress.getBroadcast();
            if (broadcast != null) {
                return broadcast;
            }
        }

        throw new UnknownHostException("无法找到可用的广播地址");
    }

    // 发送消息的方法，使用 message 类型作为参数
    public static void sendMessage(message msg) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] msgBytes = new Gson().toJson(msg).getBytes(StandardCharsets.UTF_8);
            DatagramPacket dp = new DatagramPacket(
                    msgBytes, msgBytes.length,
                    msg.getDestinationIP(), msg.getDestinationPort()
            );
            socket.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastMessageInPlayers(player[] players, String label, String content) {
        for (player player : players) {
            if(player != null){
                try {
                    sendMessage(new message(label, InetAddress.getLocalHost(), SERVER_PORT,
                            InetAddress.getByName(player.getPlayerIP()), CLIENT_PORT, content));
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
