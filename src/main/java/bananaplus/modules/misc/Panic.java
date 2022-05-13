package bananaplus.modules.misc;

import bananaplus.modules.BananaPlus;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.util.ArrayList;

public class Panic extends Module {

    // Made this module cuz sometimes when u crash u can't rejoin and type .panic fast enough so doing it at the main menu
    public Panic() {
        super(BananaPlus.MISC, "panic", "Turns off all modules that are active.");
    }

    @Override
    public void onActivate() {
        new ArrayList<>(Modules.get().getActive()).forEach(Module::toggle);
    }

}
