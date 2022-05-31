package bananaplus.modules.misc;

import bananaplus.modules.BananaPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;

import static bananaplus.utils.TimerUtils.getTPSMatch;

public class TPSSync extends Module {

    public TPSSync() {
        super(BananaPlus.MISC, "tps-sync", "Adds a general TPS Sync module.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) return;

        Modules.get().get(Timer.class).setOverride(getTPSMatch(true));
    }
}
