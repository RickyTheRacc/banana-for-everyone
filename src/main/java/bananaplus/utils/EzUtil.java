package bananaplus.utils;

import bananaplus.modules.hud.stats.*;
import bananaplus.modules.combat.AutoEz;
import bananaplus.utils.ServerUtils.BPlusPacketUtils;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EzUtil {
    public static boolean isStatsActive() {
        for (HudElement element : HUD.get().elements) {
            if (element instanceof Kills && element.active) return true;
            if (element instanceof Deaths && element.active) return true;
            if (element instanceof HighScore && element.active) return true;
            if (element instanceof KillStreak && element.active) return true;
            if (element instanceof KD && element.active) return true;
        }

        return false;
    }

    public static boolean shouldListen() {
        return isStatsActive() || Modules.get().get(AutoEz.class).isActive();
    }

    public static boolean isSelf() {
        return BPlusPacketUtils.deadEntity == mc.player;
    }

    public static boolean isTarget() {
        for (Module module : Modules.get().getAll()) {
            if (module.getInfoString() != null) {
                if (module.getInfoString().contains(BPlusPacketUtils.deadEntity.getEntityName())) return true;
            }
        }

        return false;
    }

    public static String stringKD() {
        if (StatsUtils.deaths < 2) return StatsUtils.kills + ".00";
        else {
            Double doubleKD = (double) StatsUtils.kills / StatsUtils.deaths;
            return String.format("%.2f", doubleKD);
        }
    }

    public static void checkCope() {
        AutoEz autoEz = Modules.get().get(AutoEz.class);
        if (autoEz.autoCope.get() && autoEz.isActive()) autoEz.sendCopeMessage();
    }

    public static void checkEz() {
        AutoEz autoEz = Modules.get().get(AutoEz.class);
        if (autoEz.autoEz.get() && autoEz.isActive()) autoEz.sendEzMessage();
    }
}
