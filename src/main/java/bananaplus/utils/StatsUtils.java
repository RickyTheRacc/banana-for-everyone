package bananaplus.utils;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

import static bananaplus.utils.BPlusEntityUtils.deadEntity;
import static bananaplus.utils.BPlusEntityUtils.isDeathPacket;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class StatsUtils {
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(StatsUtils.class);
    }


    public static Integer kills = 0;
    public static Integer deaths = 0;
    public static Integer highScore = 0;
    public static Integer killStreak = 0;

    private static int ticksPassed;
    public static int crystalsPerSec;
    public static int first;


    public static String KDR() {
        if (StatsUtils.deaths < 2) return StatsUtils.kills + ".00";
        else {
            Double doubleKD = (double) StatsUtils.kills / StatsUtils.deaths;
            return String.format("%.2f", doubleKD);
        }
    }

    public static boolean isTarget() {
        for (Module module : Modules.get().getAll()) {
            if (module.getInfoString() != null) {
                if (module.getInfoString().contains(deadEntity.getEntityName())) return true;
            }
        }
        return false;
    }


    // Kill Stats
    @EventHandler
    private static void onReceivePacket(PacketEvent.Receive event) {
        if (isDeathPacket(event)) {
            if (deadEntity == mc.player) {
                deaths++;
                killStreak = 0;
            }

            if (isTarget()) {
                kills++;
                killStreak++;
                highScore++;
            }
        }
    }

    @EventHandler
    private static void onGameJoin(GameJoinedEvent event) {
        kills = 0;
        deaths = 0;
        highScore = 0;
        killStreak = 0;
    }


    // Crystals per Second
    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        if (ticksPassed < 21) ticksPassed++;
            else {
                ticksPassed = 0;
            }

            if (ticksPassed == 1) first = InvUtils.find(Items.END_CRYSTAL).count();

            if (ticksPassed == 21) {
                int second = InvUtils.find(Items.END_CRYSTAL).count();
                int difference = -(second - first);
                crystalsPerSec = Math.max(0, difference);
            }
        }
    }