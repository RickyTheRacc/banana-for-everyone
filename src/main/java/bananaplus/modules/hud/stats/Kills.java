package bananaplus.modules.hud.stats;

import bananaplus.utils.StatsUtils;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.DoubleTextHudElement;

public class Kills extends DoubleTextHudElement {
    public Kills(HUD hud) {
        super(hud, "Kills", "Displays your total kill count.", "Kills: ");
    }

    @Override
    protected String getRight() {
        return String.valueOf(StatsUtils.kills);
    }
}
