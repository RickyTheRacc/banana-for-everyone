package me.ricky.banana.utils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.ricky.banana.events.DeathEvent;
import me.ricky.banana.events.JoinEvent;
import me.ricky.banana.events.LeaveEvent;
import me.ricky.banana.events.PopEvent;
import me.ricky.banana.systems.BananaUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class PlayerUtil extends BananaUtils {
    private static final ArrayList<Class<? extends Module>> singleTargets = new ArrayList<>();
    private static final ArrayList<Class<? extends Module>> multiTargets = new ArrayList<>();

    private static final ArrayList<PlayerListEntry> lastEntries = new ArrayList<>();
    private static final Object2LongMap<UUID> targets = new Object2LongOpenHashMap<>();
    private static final Object2IntMap<UUID> totemPops = new Object2IntOpenHashMap<>();

    @PostInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(PlayerUtil.class);

        // Cache the classes of modules that target players in some way
        // Should work for every module on current version AFAIK

        for (Module module: Modules.get().getAll()) {
            Class<? extends Module> clazz = module.getClass();

            try {
                Field target = clazz.getDeclaredField("target");
                if (target.getType() != PlayerEntity.class) continue;
                if (Modifier.isFinal(target.getModifiers())) continue;

                singleTargets.add(clazz);
            } catch (Exception ignored) {}

            try {
                Field targets = clazz.getDeclaredField("targets");
                if (!targets.getType().isAssignableFrom(List.class)) continue;

                if (targets.getGenericType() instanceof ParameterizedType type) {
                    Type[] typeArguments = type.getActualTypeArguments();
                    if (typeArguments.length == 0) continue;
                    if (typeArguments[0] != PlayerEntity.class) continue;
                } else continue;

                multiTargets.add(clazz);
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        totemPops.clear();
        targets.clear();
        lastEntries.clear();

        lastEntries.addAll(mc.getNetworkHandler().getPlayerList());
    }

    @EventHandler
    private static void onPreTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        try {
            for (Class<? extends Module> clazz: singleTargets) {
                Field player = clazz.getDeclaredField("target");
                player.setAccessible(true);

                PlayerEntity target = (PlayerEntity) player.get(Modules.get().get(clazz));
                if (target != null) targets.putIfAbsent(target.getUuid(), System.currentTimeMillis());
            }

            for (Class<? extends Module> clazz: multiTargets) {
                Field players = clazz.getDeclaredField("targets");
                players.setAccessible(true);

                List<PlayerEntity> list = (List<PlayerEntity>) players.get(Modules.get().get(clazz));
                if (list == null || list.isEmpty()) continue;
                list.forEach(player -> targets.putIfAbsent(player.getUuid(), System.currentTimeMillis()));
            }
        } catch (Exception ignored) {}

        targets.values().removeIf(timeAdded -> System.currentTimeMillis() - timeAdded >= 3000);
    }

    @EventHandler
    private static void onPostTick(TickEvent.Post event) {
        if (!Utils.canUpdate()) return;

        ArrayList<PlayerListEntry> currentEntries = new ArrayList<>(mc.getNetworkHandler().getPlayerList());
        currentEntries.removeIf(entry -> {
            if (entry.getGameMode() == null) return true;
            return entry.getDisplayName() == null;
        });

        // Leaving players

        for (PlayerListEntry entry: currentEntries) {
            if (currentEntries.contains(entry)) continue;

            boolean wasTarget = targets.containsKey(entry.getProfile().getId());
            MeteorClient.EVENT_BUS.post(LeaveEvent.get(entry, wasTarget));
        }

        // Joining players

        for (PlayerListEntry entry: lastEntries) {
            if (currentEntries.contains(entry)) continue;
            MeteorClient.EVENT_BUS.post(JoinEvent.get(entry));
        }

        lastEntries.clear();
        lastEntries.addAll(currentEntries);
    }

    @EventHandler
    private static void onPacketReceive(PacketEvent.Receive event) {
        if (!Utils.canUpdate()) return;

        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;
        if (!(packet.getEntity(mc.world) instanceof PlayerEntity player)) return;

        // Totem pops

        if (packet.getStatus() == 35) {
            int pops = totemPops.getOrDefault(player.getUuid(), 0) + 1;
            boolean wasTarget = targets.containsKey(player.getUuid());
            MeteorClient.EVENT_BUS.post(PopEvent.get(player, pops, wasTarget));

            totemPops.put(player.getUuid(), pops);
        }

        // Deaths

        if (packet.getStatus() == 3) {
            int pops = totemPops.removeInt(player.getUuid());
            boolean wasTarget = targets.containsKey(player.getUuid());
            MeteorClient.EVENT_BUS.post(DeathEvent.get(player, pops, wasTarget));

            targets.removeLong(player.getUuid());
        }
    }

    public static int getPops(PlayerEntity player) {
        return totemPops.getOrDefault(player.getUuid(), 0);
    }

    public static boolean isTarget(PlayerEntity player) {
        return targets.containsKey(player.getUuid());
    }
}
