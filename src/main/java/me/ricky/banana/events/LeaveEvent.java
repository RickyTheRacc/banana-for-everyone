package me.ricky.banana.events;

import net.minecraft.client.network.PlayerListEntry;

public class LeaveEvent {
    private static final LeaveEvent INSTANCE = new LeaveEvent();

    public PlayerListEntry entry;
    public boolean wasTarget;

    public static LeaveEvent get(PlayerListEntry entry, boolean wasTarget) {
        INSTANCE.entry = entry;
        INSTANCE.wasTarget = wasTarget;
        return INSTANCE;
    }
}
