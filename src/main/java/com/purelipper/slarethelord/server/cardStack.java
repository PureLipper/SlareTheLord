package com.purelipper.slarethelord.server;

import com.google.gson.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class cardStack {
    private final String name;
    private final ArrayList<cardBean> stack = new ArrayList<>();

    public cardStack(String name) {
        this.name = name;
    }

    public cardStack(cardStack cs) {
        this.name = cs.name;
        this.stack.addAll(cs.stack);
    }

    public void copyStack(cardStack cs) {
        stack.clear();
        stack.addAll(cs.getStack());
    }

    public cardStack sortByFigure() {
        final String[] figureOrder = {
                "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "joker"
        };

        stack.sort(new Comparator<>() {
            @Override
            public int compare(cardBean c1, cardBean c2) {
                return Integer.compare(
                        indexOfFigure(c1.getFigure(), figureOrder),
                        indexOfFigure(c2.getFigure(), figureOrder)
                );
            }

            // 获取 figure 在 figureOrder 中的索引位置
            private int indexOfFigure(String figure, String[] order) {
                for (int i = 0; i < order.length; i++) {
                    if (order[i].equals(figure)) {
                        return i;
                    }
                }
                return -1; // 默认情况，应该不会发生
            }
        });

        return this;
    }

    public void removeCard(String figure){
        for (int i = 0; i < stack.size(); i++) {
            if (stack.get(i).getFigure().equals(figure)) {
                stack.remove(i);
                break;
            }
        }
    }

    public static cardStack genCardStackByJson(String name, String path) {
        cardStack stack = new cardStack(name);
        Gson gson = new Gson();
        try {
            JsonObject json = gson.fromJson(new FileReader(path), JsonObject.class);
            for (JsonElement figure : json.get("hearts").getAsJsonArray()) {
                stack.insertCardToTop(new cardBean(figure.getAsString(), cardBean.HEARTS));
            }
            for (JsonElement figure : json.get("diamonds").getAsJsonArray()) {
                stack.insertCardToTop(new cardBean(figure.getAsString(), cardBean.DIAMONDS));
            }
            for (JsonElement figure : json.get("clubs").getAsJsonArray()) {
                stack.insertCardToTop(new cardBean(figure.getAsString(), cardBean.CLUBS));
            }
            for (JsonElement figure : json.get("spades").getAsJsonArray()) {
                stack.insertCardToTop(new cardBean(figure.getAsString(), cardBean.SPADES));
            }
            for (int i = 0; i < json.get("joker").getAsInt(); i++) {
                stack.insertCardToTop(new cardBean("joker", cardBean.JOKER));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return stack;
    }

    public void insertCardToTop(cardBean c) {
        stack.add(c);
    }

    public void insertNCardsToTop(cardBean c, int n) {
        for (int i = 0; i < n; i++) {
            stack.add(c);
        }
    }

    public void insertCardToBottom(cardBean c) {
        stack.addFirst(c);
    }

    public void insertNCardsToBottom(cardBean c, int n) {
        for (int i = 0; i < n; i++) {
            stack.addFirst(c);
        }
    }

    public cardStack shuffle() {
        Collections.shuffle(stack);
        return this;
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    public void clear() {
        stack.clear();
    }

    public cardBean getTopCard() {
        if (isEmpty()) {
            throw new IllegalStateException("Card stack is empty.");
        }
        return stack.removeLast();
    }

    public cardBean getBottomCard() {
        if (isEmpty()) {
            throw new IllegalStateException("Card stack is empty.");
        }
        return stack.removeFirst();
    }

    public ArrayList<cardBean> getStack() {
        return stack;
    }

    public String getName() {
        return name;
    }

    public void deleteOne(String figure,int suit){
        for (cardBean cardBean : stack) {
            if(cardBean.getFigure().equals(figure) && cardBean.getSuit() == suit){
                stack.remove(cardBean);
                break;
            }
        }
    }

    public static cardStack parseStackFromJson(String json) throws JsonParseException {
        if (json == null || json.equals("null")) {
            return null; // 避免解析错误
        }
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("name") ||
                !obj.has("stack")) {
            throw new JsonParseException("stack is missing or invalid");
        }
        String name = obj.get("name").getAsString();
        JsonArray arr = obj.get("stack").getAsJsonArray();
        cardStack stack = new cardStack(name);
        for (JsonElement e : arr) {
            stack.insertCardToTop(cardBean.parseCardFromJson(e.getAsString()));
        }
        return stack;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("cards in stack:\n");
        for (cardBean c : stack) {
            str.append(c.toString());
            str.append('\n');
        }
        return str.toString();
    }

}
