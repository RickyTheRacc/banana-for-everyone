package bananaplus.modules.hud.stats;

import bananaplus.utils.StatsUtils;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.DoubleTextHudElement;

public class KillStreak extends DoubleTextHudElement {
    public KillStreak(HUD hud) {
        super(hud, "KillStreak", "Displays your current killStreak.", "KillStreak: ");
    }

    @Override
    protected String getRight() {
        return String.valueOf(StatsUtils.killStreak);
    }
}

