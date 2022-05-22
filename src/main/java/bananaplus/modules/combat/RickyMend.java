package bananaplus.modules.combat;

import bananaplus.modules.BananaPlus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;

public class RickyMend extends Module {
    public enum ListenMode{
        Keybind,
        Automatic
    }

    public enum SwitchMode {
        Normal,
        Silent,
        None
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayer = settings.createGroup("Player");
    private final SettingGroup sgPause = settings.createGroup("Pause");


    // General
    private final Setting<ListenMode>  listenMode = sgGeneral.add(new EnumSetting.Builder<ListenMode> ()
            .name("listen-mode")
            .description("When the module should activate.")
            .defaultValue(ListenMode.Keybind)
            .build()
    );

    private final Setting<Keybind> throwBind = sgGeneral.add(new KeybindSetting.Builder()
            .name("force-keybind")
            .description("The keybind to throw XP.")
            .defaultValue(Keybind.none())
            .visible(() -> listenMode.get() == ListenMode.Keybind)
            .build()
    );


    private final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-toggle")
            .description("Automatically turn off when your items are repaired.")
            .defaultValue(true)
            .visible(() -> listenMode.get() == ListenMode.Automatic)
            .build()
    );

    private final Setting<Boolean> replenish = sgGeneral.add(new BoolSetting.Builder()
            .name("replenish")
            .description("Automatically mode XP into your hotbar.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> hotbarSlot = sgGeneral.add(new IntSetting.Builder()
            .name("hotbar-slot")
            .description("How damaged an item must be to activate.")
            .defaultValue(5)
            .range(1,9)
            .sliderRange(1,9)
            .visible(replenish::get)
            .build()
    );

    private final Setting<Integer> minThreshold = sgGeneral.add(new IntSetting.Builder()
            .name("min-durability")
            .description("How damaged an item must be to activate.")
            .defaultValue(20)
            .range(1,99)
            .sliderRange(1,99)
            .visible(() -> listenMode.get() == ListenMode.Automatic)
            .build()
    );

    private final Setting<Integer> maxThreshold = sgGeneral.add(new IntSetting.Builder()
            .name("max-durability")
            .description("The maximum durability to repair items to.")
            .defaultValue(80)
            .range(1,100)
            .sliderRange(1,100)
            .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Send info about the module's status and your armor's durability.")
            .defaultValue(true)
            .build()
    );


    // Player
    private final Setting<SwitchMode>  switchMode = sgPlayer.add(new EnumSetting.Builder<SwitchMode> ()
            .name("switch-mode")
            .description("When the module should activate.")
            .defaultValue(SwitchMode.Silent)
            .build()
    );

    private final Setting<Boolean> gapSwitch = sgPlayer.add(new BoolSetting.Builder()
            .name("gap-switch")
            .description("Whether to switch to XP if you're holding a gap.")
            .defaultValue(true)
            .visible(() -> switchMode.get() == SwitchMode.Normal)
            .build()
    );

    private final Setting<Integer> throwDelay = sgPlayer.add(new IntSetting.Builder()
            .name("throw-delay")
            .description("How fast to throw XP.")
            .defaultValue(5)
            .range(1,20)
            .sliderRange(1,20)
            .build()
    );

    private final Setting<Boolean> lookDown = sgPlayer.add(new BoolSetting.Builder()
            .name("look-down")
            .description("Forces you to rotate downwards when throwing XP.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgPlayer.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only allows when you are on the ground.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyInHole = sgPlayer.add(new BoolSetting.Builder()
            .name("only-in-hole")
            .description("Only allows when you are in a hole.")
            .defaultValue(false)
            .build()
    );


    //Pause
    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Whether to pause while eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Whether to pause while eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Whether to pause while eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> minHealth = sgPause.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("Minimum health for Auto XP.")
            .defaultValue(10)
            .range(0,20)
            .sliderRange(0,20)
            .build()
    );


    public RickyMend() {
        super(BananaPlus.MISC, "wip-xp-thrower", "No idea when this will be finished but it will be epic");
    }
}
