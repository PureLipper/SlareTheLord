package com.purelipper.slarethelord.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.purelipper.slarethelord.server.bossBean;
import com.purelipper.slarethelord.server.cardBean;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

import static com.purelipper.slarethelord.server.cardBean.*;

public class cardCalculator {
    public static final Map<String, Integer> cardPoint = new HashMap<>();
    static {
        try {
            JsonObject cardPoints = new Gson().fromJson(new FileReader("src/main/resources/cardPoint.json"), JsonObject.class);
            for (String s : cardPoints.keySet()) {
                int t = cardPoints.get(s).getAsInt();
                cardPoint.put(s, t);
            }
//            System.out.println("Card Points: " + cardPoint);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean legalityCheck(ArrayList<cardBean> cards) {
        if(cards.isEmpty()){
            return false;
        }
        if (cards.size() != 1) {
            if(containsFigure(cards,"joker")){
                return false;
            }
            if (containsFigure(cards,"A")) {
                ArrayList<cardBean> temp = new ArrayList<>();
                for (cardBean cardBean : cards) {
                    if (!cardBean.getFigure().equals("A")) {
                        temp.add(cardBean);
                    }
                }
                if (temp.isEmpty()) {
                    return false;
                }
                if (temp.size() == 1) {
                    return true;
                }
                return false;
            } else {
                //判定是否为combo
                cardBean c = cards.getFirst();
                for (cardBean card : cards) {
                    if (!Objects.equals(c.getFigure(), card.getFigure())) {
                        return false;
                    }
                }
                int sum = 0;
                for (cardBean card : cards) {
                    sum += cardPoint.get(card.getFigure());
                }
                if (sum > 10) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean containsCard(ArrayList<cardBean> cards, int suit, String figure) {
        for (cardBean card : cards) {
            if (card.getFigure().equals(figure) && card.getSuit() == suit) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsFigure(ArrayList<cardBean> cards, String figure) {
        for (cardBean card : cards) {
            if (card.getFigure().equals(figure)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsSuit(ArrayList<cardBean> cards, int suit) {
        for (cardBean card : cards) {
            if (card.getSuit() == suit) {
                return true;
            }
        }
        return false;
    }

    public static int calculatePoints(ArrayList<cardBean> cards) {
        int sum = 0;
        for (cardBean card : cards) {
            sum += cardPoint.get(card.getFigure());
        }
        return sum;
    }

    public static boolean[] calculateEffect(ArrayList<cardBean> cards, bossBean currentBoss) {
        boolean[] suitFlag = {false,false,false,false,false};
        for (cardBean card : cards) {
            switch (card.getSuit()) {
                case JOKER -> suitFlag[JOKER] = true;
                case HEARTS -> suitFlag[HEARTS] = true;
                case DIAMONDS -> suitFlag[DIAMONDS] = true;
                case CLUBS -> suitFlag[CLUBS] = true;
                case SPADES -> suitFlag[SPADES] = true;
            }
        }
        if(currentBoss.isActivated()){
            suitFlag[currentBoss.getSuit()] = false;
        }
        return suitFlag;
    }
}
