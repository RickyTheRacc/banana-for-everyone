package bananaplus.fixed.combat;

import bananaplus.BananaPlus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

public class MonkeHand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTotems = settings.createGroup("Totems");
    private final SettingGroup sgGapples = settings.createGroup("Gapples");
    private final SettingGroup sgCombat = settings.createGroup("Combat");

    // General

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How many ticks to wait between movements.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0,20)
        .build()
    );

    private final Setting<Boolean> useHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("use-hotbar")
        .description("Whether to use items from your hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> usePause = sgGeneral.add(new BoolSetting.Builder()
        .name("use-pause")
        .description("Won't switch while you're using items in your mainhand.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> spoofScreen = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-screen")
        .description("Spoof closing your inventory after each movement, can trick some stricter ACs.")
        .defaultValue(true)
        .build()
    );

    // Totems

    private final Setting<TotemMode> totemMode = sgTotems.add(new EnumSetting.Builder<TotemMode>()
        .name("autototem-mode")
        .description("Strict will always hold a totem.")
        .defaultValue(TotemMode.Smart)
        .build()
    );

    private final Setting<Double> minHealth = sgTotems.add(new DoubleSetting.Builder()
        .name("min-health")
        .description("How low you have to be to hold a totem.")
        .defaultValue(10)
        .range(0,36)
        .sliderRange(0,36)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Double> armorOffset = sgTotems.add(new DoubleSetting.Builder()
        .name("armor-offset")
        .description("Raise your min health by this amount for each piece of armor you're missing.")
        .defaultValue(2)
        .range(0,36)
        .sliderRange(0,36)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Double> holeOffset = sgTotems.add(new DoubleSetting.Builder()
        .name("hole-offset")
        .description("Lower your min health by this amount if you're in a safe hole.")
        .defaultValue(4)
        .range(0,36)
        .sliderRange(0,36)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> elytraCheck = sgTotems.add(new BoolSetting.Builder()
        .name("elytra-check")
        .description("Always hold a totem while using elytra.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> fallingCheck = sgTotems.add(new BoolSetting.Builder()
        .name("falling-check")
        .description("Hold a totem if fall damage could kill you.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> damageCheck = sgTotems.add(new BoolSetting.Builder()
        .name("damage-check")
        .description("Hold a totem if you could take fatal damage next tick.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    // Gapples

    private final Setting<Boolean> rightClickGap = sgGapples.add(new BoolSetting.Builder()
        .name("right-click")
        .description("Only hold gapples while holding your use key.")
        .defaultValue(true)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> alwaysOnWeapon = sgGapples.add(new BoolSetting.Builder()
        .name("always-on-weapon")
        .description("Always hold gapples while holding a weapon.")
        .defaultValue(true)
        .visible (() -> totemMode.get() != TotemMode.Strict && rightClickGap.get())
        .build()
    );

    private final Setting<Boolean> preferCrapples = sgGapples.add(new BoolSetting.Builder()
        .name("prefer-crapples")
        .description("Prefer holding regular gapples over egaps.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> onlyInHoles = sgGapples.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Only allow you to gap if you're in a hole.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    // Combat

    public MonkeHand() {
        super(BananaPlus.FIXED, "monkehand", "The best offhand in the game. Even works with XCarry!");
    }

    public enum TotemMode {
        Smart,
        Strict
    }
}
