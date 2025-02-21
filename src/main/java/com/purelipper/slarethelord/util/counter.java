package com.purelipper.slarethelord.util;

public class counter{
    private final int seconds;
    private int count = 0;
    private boolean interrupted = false;

    public counter(int seconds){
        this.seconds = seconds;
    }

    public void start(){
        interrupted = false;
        try {
            while(count < seconds && !interrupted){
                count++;
                Thread.sleep(1000);
            }
            interrupted = true;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop(){
        interrupted = true;
    }

    public void reset(){
        count = 0;
    }
}