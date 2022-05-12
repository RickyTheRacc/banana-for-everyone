package bananaplus.modules.hud.stats;

import bananaplus.utils.EzUtil;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.DoubleTextHudElement;

public class KD extends DoubleTextHudElement {
    public KD(HUD hud) {
        super(hud, "Kill/Death", "Displays your kills to death ratio", "K/D: ");
    }

    @Override
    protected String getRight() {
        return (isInEditor() ? "0.00" : EzUtil.stringKD());
    }

}