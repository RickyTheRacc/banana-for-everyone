package me.ricky.banana.utils;

import me.ricky.banana.events.DeathEvent;
import me.ricky.banana.systems.BananaUtils;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.value.Value;
import net.minecraft.item.Items;

import static java.lang.StringTemplate.STR;

public class StatsUtil extends BananaUtils {
    public static Integer kills = 0;
    public static Integer deaths = 0;
    public static Integer highScore = 0;
    public static Integer killStreak = 0;

    private static int ticksPassed;
    public static int crystalsPerSec;
    public static int first;

    @EventHandler
    private static void onGameJoin(GameJoinedEvent event) {
        kills = 0;
        deaths = 0;
        highScore = 0;
        killStreak = 0;
    }

    @EventHandler
    private static void onPreTick(TickEvent.Pre event) {
        if (!Utils.canUpdate() || ++ticksPassed % 21 != 1) return;
        int crystals = InvUtils.find(Items.END_CRYSTAL).count();

        if (ticksPassed == 1) {
            first = crystals;
        } else if (ticksPassed == 21) {
            crystalsPerSec = Math.max(0, -(crystals - first));
            ticksPassed = 0;
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
        if (deaths <= 0) return Value.string(STR."\{kills}.00");
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
}
