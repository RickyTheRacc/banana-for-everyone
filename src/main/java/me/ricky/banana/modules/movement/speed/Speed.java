package me.ricky.banana.modules.movement.speed;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Speed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgVanilla = settings.createGroup("Vanilla");
    private final SettingGroup sgNoCheat = settings.createGroup("NoCheat");
    private final SettingGroup sgTimer = settings.createGroup("NoCheat");
    private final SettingGroup sgPotions = settings.createGroup("Potions");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgAnticheat = settings.createGroup("Anticheat");

    // General

    private final Setting<Mode> speedMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("speed-mode")
        .description("How to apply speed to the player.")
        .defaultValue(Mode.NoCheat)
        .build()
    );

    private final Setting<RubberbandMode> rubberbandMode = sgGeneral.add(new EnumSetting.Builder<RubberbandMode>()
        .name("rubberband")
        .description("What to do when you're rubberbanded.")
        .defaultValue(RubberbandMode.Pause)
        .build()
    );

    private final Setting<Integer> pauseTime = sgGeneral.add(new IntSetting.Builder()
        .name("pause-time")
        .description("How many ticks you are allowed to tick shift for.")
        .defaultValue(60)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    // Vanilla

    private final Setting<Double> vSneakSpeed = sgVanilla.add(new DoubleSetting.Builder()
        .name("sneak-speed")
        .description("The speed in blocks per second (on ground and sneaking).")
        .defaultValue(2.6)
        .min(0)
        .sliderMax(20)
        .visible(() -> speedMode.get() == Mode.Vanilla)
        .build()
    );

    private final Setting<Double> vGroundSpeed = sgVanilla.add(new DoubleSetting.Builder()
        .name("ground-speed")
        .description("The speed in blocks per second (on ground).")
        .defaultValue(5.6)
        .min(0)
        .sliderMax(20)
        .visible(() -> speedMode.get() == Mode.Vanilla)
        .build()
    );

    private final Setting<Double> vAirSpeed = sgVanilla.add(new DoubleSetting.Builder()
        .name("air-speed")
        .description("The speed in blocks per second (on air).")
        .defaultValue(6)
        .min(0)
        .sliderMax(20)
        .visible(() -> speedMode.get() == Mode.Vanilla)
        .build()
    );

    private final Setting<Double> vWaterSpeed = sgVanilla.add(new DoubleSetting.Builder()
        .name("air-speed")
        .description("The speed in blocks per second (on air).")
        .defaultValue(6)
        .min(0)
        .sliderMax(20)
        .visible(() -> speedMode.get() == Mode.Vanilla)
        .build()
    );

    private final Setting<Double> vGuhSpeed = sgVanilla.add(new DoubleSetting.Builder()
        .name("air-speed")
        .description("The speed in blocks per second (on air).")
        .defaultValue(6)
        .min(0)
        .sliderMax(20)
        .visible(() -> speedMode.get() == Mode.Vanilla)
        .build()
    );

    private final Setting<Boolean> vanillaTimer = sgVanilla.add(new BoolSetting.Builder()
        .name("use-timer")
        .description("Use timer to boost your speed.")
        .defaultValue(false)
        .visible(() -> speedMode.get() == Mode.Vanilla)
        .build()
    );

    // Nocheat

    // Timer

    // Potions

    // Pause

    // Anticheat


    public Speed() {
        super(Categories.Movement, "speed", "Increase the speed and control with which you move.");
    }

    enum Mode {
        Vanilla,
        NoCheat,
        Timer
    }

    enum RubberbandMode {
        Reset,
        Pause,
        Slow,
        None
    }
}
