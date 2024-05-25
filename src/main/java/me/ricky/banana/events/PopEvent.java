package me.ricky.banana.events;

import net.minecraft.entity.player.PlayerEntity;

public class PopEvent {
    private static final PopEvent INSTANCE = new PopEvent();

    public PlayerEntity player;
    public int pops;
    public boolean wasTarget;

    public static PopEvent get(PlayerEntity player, int pops, boolean wasTarget) {
        INSTANCE.player = player;
        INSTANCE.pops = pops;
        INSTANCE.wasTarget = wasTarget;
        return INSTANCE;
    }
}
