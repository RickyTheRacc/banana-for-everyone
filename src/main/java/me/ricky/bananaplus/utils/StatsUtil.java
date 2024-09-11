package me.ricky.bananaplus.utils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.ricky.bananaplus.events.DeathEvent;
import me.ricky.bananaplus.events.PopEvent;
import me.ricky.bananaplus.systems.BananaUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.value.Value;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class StatsUtil extends BananaUtils {
    private static final ArrayList<Class<? extends Module>> singleTargets = new ArrayList<>();
    private static final ArrayList<Class<? extends Module>> multiTargets = new ArrayList<>();

    public static final Object2LongMap<UUID> targets = new Object2LongOpenHashMap<>();
    public static final Object2IntMap<UUID> totemPops = new Object2IntOpenHashMap<>();

    public static int kills = 0;
    public static int deaths = 0;
    public static int highScore = 0;
    public static int killStreak = 0;

    private static double countDelay;
    public static int crystalsPerSec;
    private static int first;

    @PostInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(StatsUtil.class);

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
    private static void onGameJoin(GameJoinedEvent event) {
        targets.clear();
        totemPops.clear();

        countDelay = 0.0;

        kills = 0;
        deaths = 0;
        highScore = 0;
        killStreak = 0;
    }

    @EventHandler
    private static void onPreTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        targets.values().removeIf(timeAdded -> System.currentTimeMillis() - timeAdded >= 3000);

        for (Class<? extends Module> clazz: singleTargets) {
            try {
                Field player = clazz.getDeclaredField("target");
                player.setAccessible(true);

                PlayerEntity target = (PlayerEntity) player.get(Modules.get().get(clazz));
                if (target != null) targets.putIfAbsent(target.getUuid(), System.currentTimeMillis());
            } catch (Exception ignored) {}
        }

        for (Class<? extends Module> clazz: multiTargets) {
            try {
                Field players = clazz.getDeclaredField("targets");
                players.setAccessible(true);

                List<PlayerEntity> list = (List<PlayerEntity>) players.get(Modules.get().get(clazz));
                if (list == null || list.isEmpty()) continue;
                list.forEach(player -> targets.putIfAbsent(player.getUuid(), System.currentTimeMillis()));
            } catch (Exception ignored) {}
        }

        countDelay -= TickRate.INSTANCE.getTickRate() / 20.0;

        if (countDelay == 20.0) {
            first = InvUtils.find(Items.END_CRYSTAL).count();
            return;
        }

        if (countDelay <= 0) {
            int crystals = InvUtils.find(Items.END_CRYSTAL).count();
            crystalsPerSec = Math.max(0, -(crystals - first));
            countDelay = 20.0;
        }
    }

    @EventHandler
    private static void onPacketReceive(PacketEvent.Receive event) {
        if (!Utils.canUpdate()) return;

        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;
        if (!(packet.getEntity(mc.world) instanceof PlayerEntity player)) return;

        UUID uuid = player.getUuid();

        // Totem pops

        if (packet.getStatus() == 35) {
            int pops = totemPops.getOrDefault(uuid, 0) + 1;
            boolean wasTarget = targets.containsKey(uuid);
            totemPops.put(uuid, pops);

            MeteorClient.EVENT_BUS.post(PopEvent.get(player, pops, wasTarget));
        }

        // Deaths

        if (packet.getStatus() == 3) {
            int pops = totemPops.removeInt(uuid);
            boolean wasTarget = targets.containsKey(uuid);
            targets.removeLong(uuid);

            MeteorClient.EVENT_BUS.post(DeathEvent.get(player, pops, wasTarget));
        }
    }

    @EventHandler
    public static void onDeath(DeathEvent event) {
        if (event.wasTarget) {
            kills++;
            killStreak++;
            highScore = Math.max(highScore, killStreak);
        } else if (event.player == mc.player) {
            deaths++;
            killStreak = 0;
        }
    }

    public static Value getKills() {
        return Value.number(kills);
    }

    public static Value getDeaths() {
        return Value.number(kills);
    }

    public static Value getKDR() {
        if (deaths <= 0) return Value.string(kills + ".00");
        return Value.string(String.format("%.2f", (double) kills / deaths));
    }

    public static Value getKillstreak() {
        return Value.number(killStreak);
    }

    public static Value getHighscore() {
        return Value.number(highScore);
    }

    public static Value getCrystalsPs() {
        return Value.number(crystalsPerSec);
    }

    public static Value getPing() {
        return Value.number(ServerUtil.getPing());
    }

    public static int getPops(PlayerEntity player) {
        return totemPops.getOrDefault(player.getUuid(), 0);
    }

    public static boolean isTarget(PlayerEntity player) {
        return targets.containsKey(player.getUuid());
    }
}
