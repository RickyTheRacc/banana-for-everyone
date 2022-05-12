package bananaplus.modules.misc;

import bananaplus.modules.AddModule;
import meteordevelopment.meteorclient.systems.modules.Module;


public class VanillaAutoJump extends Module {
    public VanillaAutoJump() {
        super(AddModule.MISC, "vanilla-auto-jump", "Toggles vanilla auto jump in minecraft.");
    }

    @Override
    public void onActivate() {
        mc.options.autoJump = true;
    }

    @Override
    public void onDeactivate() {
        mc.options.autoJump = false;
    }

}


