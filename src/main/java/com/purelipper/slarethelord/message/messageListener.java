package com.purelipper.slarethelord.message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class messageListener {
    private final int listenPort;
    private final Thread listener;
    private final BlockingDeque<message> msgQueue = new LinkedBlockingDeque<>();

    public messageListener(int port){
        listenPort = port;
        listener = new Thread(() -> {
            try{
                DatagramSocket ds = new DatagramSocket(port);
                while (true) {
                    byte[] bys = new byte[1024];
                    DatagramPacket dp = new DatagramPacket(bys, bys.length);
                    ds.receive(dp);
                    msgQueue.put(message.parseMessageFromJson(new String(dp.getData(), 0, dp.getLength())));
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void start(){
        listener.start();
    }

    public void stop(){
        listener.interrupt();
    }

    public int getListenPort() {
        return listenPort;
    }

    public BlockingDeque<message> getMsgQueue() {
        return msgQueue;
    }

    public message nextMessage() {
        try {
            return msgQueue.take();  // 🔹 阻塞式获取消息
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
