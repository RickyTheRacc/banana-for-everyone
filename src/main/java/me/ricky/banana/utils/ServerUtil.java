package me.ricky.banana.utils;

import com.google.common.base.Stopwatch;
import me.ricky.banana.events.JoinEvent;
import me.ricky.banana.events.LeaveEvent;
import me.ricky.banana.systems.BananaUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class ServerUtil extends BananaUtils {
    private static final Set<PlayerListEntry> lastEntries = new HashSet<>();
    private static final Set<PlayerEntity> lastPlayers = new HashSet<>();

    public static final Queue<Integer> pings = new ConcurrentLinkedQueue<>();
    private static final Stopwatch timer = Stopwatch.createUnstarted();
    private static boolean checking;
    private static double pingDelay;

    @PostInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(ServerUtil.class);
    }

    @EventHandler
    private static void onGameJoin(GameJoinedEvent event) {
        lastPlayers.clear();
        lastEntries.clear();

        lastPlayers.addAll(getPlayers());
        lastEntries.addAll(getTabList());

        pingDelay = 40.0;
        checking = false;
        pings.clear();

        if (timer.isRunning()) {
            timer.stop();
            timer.reset();
        }
    }

    @EventHandler
    private static void onPreTick(TickEvent.Pre event) {
        if (!Utils.canUpdate() || checking) return;
        if ((pingDelay -= TickRate.INSTANCE.getTickRate() / 20.0) > 0) return;

        mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
        timer.start();

        checking = true;
        pingDelay = 40.0;
    }

    @EventHandler
    private static void onPacketReceive(PacketEvent.Receive event) {
        if (!Utils.canUpdate() || !checking) return;
        if (!(event.packet instanceof StatisticsS2CPacket)) return;

        checking = false;
        timer.stop();

        long actualPing = timer.elapsed(TimeUnit.MILLISECONDS);
        int ping = (int) (actualPing / (TickRate.INSTANCE.getTickRate() / 20.0));

        // Ignore impossible values
        if (ping > 0 && ping < 10000) {
            pings.add(ping);
            if (pings.size() > 20) pings.poll();
        }

        timer.reset();
    }

    @EventHandler
    private static void onPostTick(TickEvent.Post event) {
        if (!Utils.canUpdate()) return;

        Set<PlayerListEntry> currentEntries = getTabList();

        // Leaving players

        for (PlayerListEntry entry: lastEntries) {
            if (currentEntries.contains(entry)) continue;
            UUID uuid = entry.getProfile().getId();

            PlayerEntity player = lastPlayers.stream().filter(p ->
                p.getUuid().equals(uuid)
            ).findFirst().orElse(null);

            boolean wasTarget = StatsUtil.targets.removeLong(uuid) != 0;
            int pops = StatsUtil.totemPops.removeInt(uuid);

            MeteorClient.EVENT_BUS.post(LeaveEvent.get(entry, player, wasTarget ,pops));
        }

        // Joining players

        for (PlayerListEntry entry: currentEntries) {
            if (lastEntries.contains(entry)) continue;
            if (currentEntries.contains(entry)) continue;
            UUID uuid = entry.getProfile().getId();

            PlayerEntity player = getPlayers().stream().filter(p ->
                p.getUuid().equals(uuid)
            ).findFirst().orElse(null);

            MeteorClient.EVENT_BUS.post(JoinEvent.get(entry, player));
        }

        lastPlayers.clear();
        lastEntries.clear();

        lastPlayers.addAll(getPlayers());
        lastEntries.addAll(currentEntries);
    }

    public static int getPing() {
        if (!Utils.canUpdate() || mc.isInSingleplayer()) return 0;
        if (pings.size() <= 10) return PlayerUtils.getPing();
        return pings.stream().mapToInt(Integer::intValue).sum() / pings.size();
    }

    public static Set<PlayerListEntry> getTabList() {
        Set<PlayerListEntry> currentEntries = new HashSet<>(mc.getNetworkHandler().getPlayerList());

        // Attempt to ignore NPCs or holograms
        currentEntries.removeIf(entry -> {
            if (entry.getGameMode() == null) return true;
            if (entry.getProfile() == null) return true;
            String name = entry.getProfile().getName();
            return name.isBlank() || !name.matches("[a-zA-Z0-9_]+");
        });

        return currentEntries;
    }

    public static Set<PlayerEntity> getPlayers() {
        Set<PlayerEntity> currentPlayers = new HashSet<>(mc.world.getPlayers());

        // Attempt to ignore NPCs or holograms
        currentPlayers.removeIf(player -> {
            if (EntityUtils.getGameMode(player) == null) return true;
            if (player.getGameProfile() == null) return true;
            String name = player.getGameProfile().getName();
            return name.isBlank() || !name.matches("[a-zA-Z0-9_]+");
        });

        return currentPlayers;
    }
}
