package bananaplus.modules.hud.stats;

import bananaplus.modules.misc.Mystery;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.DoubleTextHudElement;

public class CrystalsPs extends DoubleTextHudElement {
    public CrystalsPs(HUD hud) {
        super(hud, "Crystals/s", "Displays your crystal drain per second", "Crystals/s: ");
    }

    @Override
    protected String getRight() {
        return String.valueOf(Mystery.crystalsPerSec);
    }
}
