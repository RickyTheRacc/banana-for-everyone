package bananaplus.utils;

import bananaplus.global.GlobalAntiCheat;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;

public class GlobalSettings extends Utils {

    private static final GlobalAntiCheat globalAntiCheat = Modules.get().get(GlobalAntiCheat.class);


    public static GlobalAntiCheat.Anticheat getAntiCheatType() {
        return globalAntiCheat.anticheatSetting.get();
    }
}
