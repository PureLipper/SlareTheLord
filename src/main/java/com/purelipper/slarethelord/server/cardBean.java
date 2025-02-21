package com.purelipper.slarethelord.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class cardBean {
    public static final int DISMISSED = -1;
    public static final int JOKER = 0;
    public static final int HEARTS = 1;
    public static final int DIAMONDS = 2;
    public static final int CLUBS = 3;
    public static final int SPADES = 4;

    private final String figure;
    private final int suit;

    public int getSuit() {
        return suit;
    }

    public String getFigure() {
        return figure;
    }

    public cardBean(String figure, int suit) {
        this.figure = figure;
        this.suit = suit;
    }

    public static cardBean parseCardFromJson(String json) throws JsonParseException {
        if (json == null || json.equals("null")) {
            return null; // 避免解析错误
        }
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("figure") ||
                !obj.has("suit")) {
            throw new JsonParseException("card is missing or invalid");
        }
        String figure = obj.get("figure").getAsString();
        int suit = obj.get("suit").getAsInt();
        return new cardBean(figure, suit);
    }

    @Override
    public String toString() {
        return suit + " " + figure;
    }
}
