package com.purelipper.slarethelord.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.purelipper.slarethelord.message.message;
import com.purelipper.slarethelord.util.NetworkUtils;
import com.purelipper.slarethelord.util.cardCalculator;
import com.purelipper.slarethelord.util.counter;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import static com.purelipper.slarethelord.server.cardBean.*;
import static com.purelipper.slarethelord.server.serverCore.CLIENT_PORT;
import static com.purelipper.slarethelord.server.serverCore.SERVER_PORT;
import static com.purelipper.slarethelord.util.cardCalculator.cardPoint;

public class gameCore {
    private final player[] currentPlayers;
    private int playerCount = 0;
    private int maxHandCards;
    private final cardStack pubCards;
    private final cardStack bossCards;
    private final cardStack discardCards;
    private final cardStack tempDiscardCards;

    private counter Counter = new counter(60);
    private bossBean currentBoss;

    private boolean gameEnded = false;

    public gameCore(player[] players) {
        currentPlayers = players;
        pubCards = cardStack.genCardStackByJson("pubCards", "src/main/resources/playerCardSet.json");
        bossCards = cardStack.genCardStackByJson("bossCards", "src/main/resources/bossCardSet.json");
        discardCards = new cardStack("discardCards");
        tempDiscardCards = new cardStack("tempDiscardCards");

        for (player player : currentPlayers) {
            if (player != null) {
                playerCount++;
            }
        }

        try {
            JsonArray gameConfig = new Gson().fromJson(new FileReader("src/main/resources/gameConfig.json"), JsonObject.class)
                    .getAsJsonArray();
            for (JsonElement e : gameConfig) {
                if (e.getAsJsonObject().get("player").getAsInt() == playerCount) {
                    for (int i = 0; i < e.getAsJsonObject().get("joker").getAsInt(); i++) {
                        pubCards.deleteOne("joker", JOKER);
                    }
                    maxHandCards = e.getAsJsonObject().get("handSize").getAsInt();
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        bossCards.shuffle().sortByFigure();
        pubCards.shuffle();
    }

    public void launch() {
        for (player currentPlayer : currentPlayers) {
            cardStack temp = new cardStack("initialCards");
            for (int i = 0; i < maxHandCards; i++) {
                temp.insertCardToTop(pubCards.getTopCard());
            }
            try {
                NetworkUtils.sendMessage(new message("initialCards", InetAddress.getLocalHost(), SERVER_PORT,
                        InetAddress.getByName(currentPlayer.getPlayerIP()), CLIENT_PORT, new Gson().toJson(temp)));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        while (!gameEnded) {
            for (player currentPlayer : currentPlayers) {
                if (currentPlayer != null) {
                    try {
                        NetworkUtils.sendMessage(new message("yourTurn", InetAddress.getLocalHost(), SERVER_PORT,
                                InetAddress.getByName(currentPlayer.getPlayerIP()), CLIENT_PORT, ""));
                        Counter.start();
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public void dealMessage(message msg) {
        switch (msg.getLabel()) {
            case "playCards" -> {
                Counter.stop();
                ArrayList<cardBean> cards = getCardsFromMsg(msg);
                boolean[] suits = cardCalculator.calculateEffect(cards, currentBoss);
                int damage = cardCalculator.calculatePoints(cards);
//                dealEffect(suits);

            }
            case "discard" -> {
                Counter.stop();
                if (!checkDefend(getCardsFromMsg(msg))) {
                    gameEnded = true;
                    for (player currentPlayer : currentPlayers) {
                        if (currentPlayer != null) {
                            try {
                                NetworkUtils.sendMessage(new message("gameOver", InetAddress.getLocalHost(), SERVER_PORT,
                                        InetAddress.getByName(currentPlayer.getPlayerIP()), CLIENT_PORT, ""));
                            } catch (UnknownHostException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
    }

    public void dealEffect(ArrayList<cardBean> cards) {
        boolean[] suits = cardCalculator.calculateEffect(cards, currentBoss);
        if (suits[JOKER]) {
            for (cardBean cardBean : tempDiscardCards.getStack()) {
                if (cardBean.getSuit() == SPADES) {
                    int defend = cardPoint.get(cardBean.getFigure());
                    if (currentBoss.getDamage() > defend) {
                        currentBoss.damageDown(defend);
                    } else {
                        currentBoss.damageDown(currentBoss.getDamage());
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (suits[HEARTS]) {
            discardCards.shuffle();
            int num = cardCalculator.calculatePoints(cards);
            for (int i = 0; i < num; i++) {
                pubCards.insertCardToBottom(discardCards.getTopCard());
            }
            System.out.println("inserted " + num + " cards into pubCards");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (suits[DIAMONDS]) {
            int num = cardCalculator.calculatePoints(cards);
            int pos = 0;
            int passCount = 0;
            for (int i = 0; i < num; i++) {
                while (true) {
                    if (currentPlayers[pos] == null || currentPlayers[pos].getHandCards().size() == maxHandCards) {
                        pos++;
                        pos = pos % 4;
                        passCount++;
                    } else {
                        break;
                    }
                    if (passCount == 4) {
                        break;
                    }
                }
                if (passCount == 4) {
                    break;
                }
                try {
                    NetworkUtils.sendMessage(new message("gameOver", InetAddress.getLocalHost(), SERVER_PORT,
                            InetAddress.getByName(currentPlayers[i].getPlayerIP()), CLIENT_PORT, ""));
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
                pos++;
                pos = pos % 4;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
//        if (suits[CLUBS]) {
//
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
        if (suits[SPADES]) {
            int point = cardCalculator.calculatePoints(cards);
            if (currentBoss.getDamage() > point) {
                currentBoss.damageDown(point);
            } else {
                currentBoss.damageDown(currentBoss.getDamage());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void dealDamage(int damage) {
        currentBoss.healthDown(damage);
        if (currentBoss.getHealth() == 0) {
            pubCards.insertCardToTop(new cardBean(currentBoss.getFigure(), currentBoss.getSuit()));
        } else if (currentBoss.getHealth() < 0) {
            discardCards.insertCardToTop(new cardBean(currentBoss.getFigure(), currentBoss.getSuit()));
        }
    }

    public boolean checkDefend(ArrayList<cardBean> cards) {
        return cardCalculator.calculatePoints(cards) >= currentBoss.getDamage();
    }

    public ArrayList<cardBean> getCardsFromMsg(message msg) {
        JsonArray arr = new Gson().fromJson(msg.getContent(), JsonArray.class);
        ArrayList<cardBean> cards = new ArrayList<>();
        for (JsonElement e : arr) {
            cards.add(cardBean.parseCardFromJson(e.getAsString()));
        }
        return cards;
    }
}
