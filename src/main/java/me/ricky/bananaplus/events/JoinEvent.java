package me.ricky.bananaplus.events;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;

import javax.annotation.Nullable;

public class JoinEvent {
    private static final JoinEvent INSTANCE = new JoinEvent();

    public PlayerListEntry entry;
    public PlayerEntity player;

    public static JoinEvent get(PlayerListEntry entry, @Nullable PlayerEntity player) {
        INSTANCE.entry = entry;
        INSTANCE.player = player;
        return INSTANCE;
    }
}
