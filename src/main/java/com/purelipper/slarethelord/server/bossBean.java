package com.purelipper.slarethelord.server;

public class bossBean {
    private final int suit;//花色
    private final String figure;
    private int damage;
    private int health;
    private boolean activated = true;

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

    public void silenced(){
        activated = false;
    }
}
