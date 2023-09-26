package bananaplus.global;

import bananaplus.BananaPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class GlobalAntiCheat extends Module {
    public enum Anticheat {
        NoCheatPlus,
        Vanilla
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Anticheat> anticheatSetting = sgGeneral.add(new EnumSetting.Builder<Anticheat>()
            .name("anti-cheats")
            .description("The anticheat mode which all modules operate on")
            .defaultValue(Anticheat.NoCheatPlus)
            .build()
    );

    public GlobalAntiCheat() {
        super(BananaPlus.GLOBAL, "GlobalAntiCheat", "What anti cheat to bypass.");
    }

}
