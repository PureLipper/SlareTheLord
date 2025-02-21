package com.purelipper.slarethelord.server;

import com.google.gson.Gson;

public class stageBean {
    private stage Stage;
    private int round;

    public enum stage{
        gameStart,
        turnStart,
        playCard,
        dealEffect,
        dealDamage,
        takeDamage,
        turnOver,
        gameOver
    }

    public stageBean(stage stage, int round) {
        this.Stage = stage;
        this.round = round;
    }

    public String getStage() {
        return Stage.name();
    }

    public int getRound() {
        return round;
    }

    public static void main(String[] args) {
        Gson gson = new Gson();
        stageBean s = new stageBean(stageBean.stage.gameStart,0);
        System.out.println(gson.toJson(s));
    }
}
