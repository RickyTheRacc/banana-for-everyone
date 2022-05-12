package bananaplus.modules.hud.stats;

import bananaplus.utils.StatsUtils;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.DoubleTextHudElement;

public class HighScore extends DoubleTextHudElement {
    public HighScore(HUD hud) {
        super(hud, "HighScore", "Displays your highest killstreak.", "HighScore: ");
    }

    @Override
    protected String getRight() {
        return String.valueOf(StatsUtils.highScore);
    }

}