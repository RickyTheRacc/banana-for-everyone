package me.ricky.banana.events;

import net.minecraft.entity.player.PlayerEntity;

public class DeathEvent {
    private static final DeathEvent INSTANCE = new DeathEvent();

    public PlayerEntity player;
    public int pops;
    public boolean wasTarget;

    public static DeathEvent get(PlayerEntity player, int pops, boolean wasTarget) {
        INSTANCE.player = player;
        INSTANCE.pops = pops;
        INSTANCE.wasTarget = wasTarget;
        return INSTANCE;
    }

}
