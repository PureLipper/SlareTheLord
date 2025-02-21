package com.purelipper.slarethelord.message;

public abstract class messageConsumer {
    private final Thread consumer;

    public messageConsumer(messageListener listener) {
        consumer = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    message msg = listener.nextMessage(); // 🔹 阻塞等待消息
                    if (msg != null) {
                        dealMessage(msg);
                    }
                }
            } catch (Exception e) {
                System.out.println("Consumer thread stopped... ");
                e.printStackTrace();
            }
        });
    }

    public abstract void dealMessage(message msg);

    public void start(){
        consumer.start();
    }

    public void stop(){
        consumer.interrupt();
    }
}
