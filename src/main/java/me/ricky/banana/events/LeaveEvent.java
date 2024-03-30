package me.ricky.banana.events;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;

import javax.annotation.Nullable;

public class LeaveEvent {
    private static final LeaveEvent INSTANCE = new LeaveEvent();

    public PlayerListEntry entry;
    public PlayerEntity player;
    public boolean wasTarget;
    public int pops;

    public static LeaveEvent get(PlayerListEntry entry, @Nullable PlayerEntity player, boolean wasTarget, int pops) {
        INSTANCE.entry = entry;
        INSTANCE.player = player;
        INSTANCE.wasTarget = wasTarget;
        INSTANCE.pops = pops;
        return INSTANCE;
    }
}
