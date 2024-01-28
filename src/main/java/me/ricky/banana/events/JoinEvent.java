package me.ricky.banana.events;

import net.minecraft.client.network.PlayerListEntry;

public class JoinEvent {
    private static final JoinEvent INSTANCE = new JoinEvent();

    public PlayerListEntry entry;

    public static JoinEvent get(PlayerListEntry entry) {
        INSTANCE.entry = entry;
        return INSTANCE;
    }
}
