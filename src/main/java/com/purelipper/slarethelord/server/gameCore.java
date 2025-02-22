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
    private int maxHandCards;
    private final cardStack pubCards;
    private final cardStack bossCards;
    private final cardStack discardCards;
    private final cardStack tempDiscardCards;

    private counter Counter = new counter(60);
    private bossBean currentBoss;

    private boolean gameEnded = false;
    private boolean joker = false;

    public gameCore(player[] players) {
        //初始化字段
        currentPlayers = players;
        pubCards = cardStack.genCardStackByJson("pubCards", "src/main/resources/playerCardSet.json");
        bossCards = cardStack.genCardStackByJson("bossCards", "src/main/resources/bossCardSet.json");
        discardCards = new cardStack("discardCards");
        tempDiscardCards = new cardStack("tempDiscardCards");

        //玩家计数
        int playerCount = 0;
        for (player player : currentPlayers) {
            if (player != null) {
                playerCount++;
            }
        }

        //根据玩家数量加载游戏配置
        try {
            JsonArray gameConfig = new Gson().fromJson(new FileReader("src/main/resources/gameConfig.json"), JsonArray.class)
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

        //初始洗牌
        bossCards.shuffle().sortByFigure();
        pubCards.shuffle();
    }

    public void launch() {
        //公告初始boss
        currentBoss = new bossBean(bossCards.getBottomCard());
        NetworkUtils.broadcastMessageInPlayers(currentPlayers, "initialBoss", new Gson().toJson(currentBoss));

        //初始发牌
        for (player currentPlayer : currentPlayers) {
            if(currentPlayer!=null){
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
        }

        //游戏循环逻辑
        while (!gameEnded) {
            for (player currentPlayer : currentPlayers) {
                if (currentPlayer != null) {
                    joker = false;
                    try {
                        //出牌阶段
                        NetworkUtils.sendMessage(new message("yourPlayTime", InetAddress.getLocalHost(), SERVER_PORT,
                                InetAddress.getByName(currentPlayer.getPlayerIP()), CLIENT_PORT, ""));
                        NetworkUtils.broadcastMessageInPlayers(currentPlayers, "playTimeAnnounce", currentPlayer.getName());
                        Counter.start();

                        //等待（效果处理阶段和造成伤害阶段）
//                        Thread.sleep(1000);

                        //弃牌阶段
                        if(!joker){
                            NetworkUtils.sendMessage(new message("yourDiscardTime", InetAddress.getLocalHost(), SERVER_PORT,
                                    InetAddress.getByName(currentPlayer.getPlayerIP()), CLIENT_PORT, ""));
                            NetworkUtils.broadcastMessageInPlayers(currentPlayers, "discardTimeAnnounce", currentPlayer.getName());
                            Counter.start();
                        }
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
                Counter.reset();
                ArrayList<cardBean> cards = getCardsFromMsg(msg);
                dealEffect(cards);
                if (!joker) {
                    dealDamage(cards);
                }
            }
            case "discard" -> {
                Counter.stop();
                Counter.reset();
                ArrayList<cardBean> cards = getCardsFromMsg(msg);
                if (!checkDefend(cards)) {
                    gameEnded = true;
                    NetworkUtils.broadcastMessageInPlayers(currentPlayers, "gameOver", "");
                } else {
                    for (player p : currentPlayers) {
                        if (p != null && p.getPlayerIP().equals(msg.getSourceIP().getHostAddress())) {
                            for (cardBean card : cards) {
                                p.getHandCards().deleteOne(card.getFigure(), card.getSuit());
                            }
                        }
                    }
                }
            }
        }
    }

    public void dealEffect(ArrayList<cardBean> cards) {
        boolean[] suits = cardCalculator.calculateEffect(cards, currentBoss);
        int damage = cardCalculator.calculatePoints(cards);
        if (suits[JOKER]) {
            //标志有joker
            joker = true;
            //沉默boss效果
            currentBoss.silenced();
            //检查临时弃牌堆中的黑桃
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
            return;
        }
        if (suits[HEARTS]) {
            discardCards.shuffle();
            for (int i = 0; i < damage; i++) {
                pubCards.insertCardToBottom(discardCards.getTopCard());
            }
            System.out.println("inserted " + damage + " cards into pubCards");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (suits[DIAMONDS]) {
            int pos = 0;
            int passCount = 0;
            for (int i = 0; i < damage; i++) {
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
    }

    public void dealDamage(ArrayList<cardBean> cards) {
        boolean[] suits = cardCalculator.calculateEffect(cards, currentBoss);
        int damage = cardCalculator.calculatePoints(cards);

        //如果是黑桃则减少boss攻击力
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

        //如果是梅花则造成双倍伤害
        if (suits[CLUBS]) {
            currentBoss.healthDown(damage * 2);
        } else {
            currentBoss.healthDown(damage);
        }

        //boss死亡判定
        if (currentBoss.getHealth() <= 0) {
            if (currentBoss.getHealth() == 0) {
                cardBean nextBoss = bossCards.getTopCard();
                if (nextBoss != null) {
                    pubCards.insertCardToTop(new cardBean(currentBoss.getFigure(), currentBoss.getSuit()));
                    currentBoss = new bossBean(nextBoss);
                    NetworkUtils.broadcastMessageInPlayers(currentPlayers, "bossConverted", "");
                } else {
                    NetworkUtils.broadcastMessageInPlayers(currentPlayers, "victory", "");
                }
            } else if (currentBoss.getHealth() < 0) {
                discardCards.insertCardToTop(new cardBean(currentBoss.getFigure(), currentBoss.getSuit()));
                NetworkUtils.broadcastMessageInPlayers(currentPlayers, "bossDied", "");
            }
            discardCards.insertCardsToTop(tempDiscardCards);
            tempDiscardCards.clear();
        }
        NetworkUtils.broadcastMessageInPlayers(currentPlayers, "syncBoss", new Gson().toJson(currentBoss));

    }

    public boolean checkDefend(ArrayList<cardBean> cards) {
        return cardCalculator.calculatePoints(cards) >= currentBoss.getDamage();
    }

    public ArrayList<cardBean> getCardsFromMsg(message msg) {
        JsonArray arr = new Gson().fromJson(msg.getContent(), JsonArray.class);
        ArrayList<cardBean> cards = new ArrayList<>();
        for (JsonElement e : arr) {
            cards.add(cardBean.parseCardFromJson(e.toString()));
        }
        return cards;
    }
}
