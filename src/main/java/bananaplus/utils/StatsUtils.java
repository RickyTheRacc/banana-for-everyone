package bananaplus.utils;

import bananaplus.utils.ServerUtils.BPlusPacketUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;

public class StatsUtils {

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(StatsUtils.class);
    }

    public static Integer kills = 0;
    public static Integer deaths = 0;
    public static Integer highScore = 0;
    public static Integer killStreak = 0;

    @EventHandler
    private static void onReceivePacket(PacketEvent.Receive event) {
        if (EzUtil.shouldListen()) {
            if (BPlusPacketUtils.isDeathPacket(event)) {
                if (EzUtil.isSelf()) {
                    deaths++;
                    killStreak = 0;
                    EzUtil.checkCope();
                }

                if (EzUtil.isTarget()) {
                    kills++;
                    killStreak++;
                    highScore++;
                    EzUtil.checkEz();
                }
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
}
