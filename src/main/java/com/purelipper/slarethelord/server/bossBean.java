package com.purelipper.slarethelord.server;

import com.google.gson.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import static com.purelipper.slarethelord.server.cardBean.*;

public class bossBean {
    private final int suit;//花色
    private final String figure;
    private int damage;
    private int health;
    private boolean activated = true;

    /**
     * 存储boss的数值信息，String是boss的figure，int[0]为伤害，int[1]为生命值
     */
    public static final Map<String, int[]> bossInfo = new HashMap<>();

    static {
        try {
            JsonArray boss = new Gson().fromJson(new FileReader("src/main/resources/bossInfo.json"), JsonArray.class);
            for (JsonElement e : boss) {
                String figure = e.getAsJsonObject().get("figure").getAsString();
                int health = e.getAsJsonObject().get("health").getAsInt();
                int damage = e.getAsJsonObject().get("damage").getAsInt();
                bossInfo.put(figure, new int[]{damage, health});
            }
//            System.out.println("boss info: " + bossInfo);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public bossBean(cardBean card) {
        if (card.getFigure().equals("J") || card.getFigure().equals("Q") || card.getFigure().equals("K")) {
            this.figure = card.getFigure();
            this.suit = card.getSuit();
            int[] t = bossInfo.get(card.getFigure());
            this.damage = t[0];
            this.health = t[1];
        }else{
            throw new IllegalArgumentException("Invalid figure: " + card.getFigure());
        }
    }

    public bossBean(int suit, String figure, int damage, int health, boolean activated) {
        this.suit = suit;
        this.figure = figure;
        this.damage = damage;
        this.health = health;
        this.activated = activated;
    }

    public bossBean(int suit, String figure, int damage, int health) {
        this.suit = suit;
        this.figure = figure;
        this.damage = damage;
        this.health = health;
    }

    public static bossBean parseBossFromJson(String json) throws JsonParseException {
        if (json == null || json.equals("null")) {
            return null; // 避免解析错误
        }
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("figure") ||
                !obj.has("suit") ||
                !obj.has("damage") ||
                !obj.has("health") ||
                !obj.has("activated")) {
            throw new JsonParseException("boss is missing or invalid");
        }
        String figure = obj.get("figure").getAsString();
        int suit = obj.get("suit").getAsInt();
        int damage = obj.get("damage").getAsInt();
        int health = obj.get("health").getAsInt();
        boolean activated = obj.get("activated").getAsBoolean();
        return new bossBean(suit,figure,damage,health,activated);
    }

    public int getSuit() {
        return suit;
    }

    public String getFigure() {
        return figure;
    }

    public int getDamage() {
        return damage;
    }

    public int getHealth() {
        return health;
    }

    public void healthDown(int damage) {
        this.health -= damage;
    }

    public void damageDown(int i) {
        this.damage -= damage;
    }

    public boolean isActivated() {
        return activated;
    }

    public void silenced() {
        activated = false;
    }

    public void show(){
        System.out.println(this);
    }

    @Override
    public String toString() {
        String s = "";
        switch (suit) {
            case JOKER -> s = "Joker";
            case HEARTS -> s = "Hearts";
            case DIAMONDS -> s = "Diamonds";
            case CLUBS -> s = "Clubs";
            case SPADES -> s = "Spades";
        }
        return "bossBean{" +
                "suit=" + s +
                ", figure='" + figure + '\'' +
                ", damage=" + damage +
                ", health=" + health +
                ", activated=" + activated +
                '}';
    }
}
