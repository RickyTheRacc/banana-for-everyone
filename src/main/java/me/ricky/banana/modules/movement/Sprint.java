package me.ricky.banana.modules.movement;

import me.ricky.banana.BananaPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Sprint extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    public final Setting <Boolean> allDirections = sgGeneral.add(new BoolSetting.Builder()
        .name("all-directions")
        .description("Allows you to sprint sideways and backwards.")
        .defaultValue(false)
        .build()
    );

    public final Setting <Boolean> preventStop = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-stop")
        .description("Makes you keep sprinting even if you run into blocks.")
        .defaultValue(false)
        .build()
    );

    public final Setting <Boolean> ignoreWater = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-water")
        .description("Try to sprint even when in water.")
        .defaultValue(true)
        .build()
    );

    public final Setting <Boolean> ignoreHunger = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-hunger")
        .description("Sprint even when below 6 hunger.")
        .defaultValue(false)
        .build()
    );

    public Sprint() {
        super(BananaPlus.CATEGORY, "sprint", "Automatically sprints.");
    }

    @EventHandler(priority = 500)
    private void onTick(TickEvent.Post event) {
        if (!canSprint()) return;
        mc.player.setSprinting(true);
    }

    public boolean canSprint() {
        if (!ignoreHunger.get() && mc.player.getHungerManager().getFoodLevel() <= 6) return false;
        if (!ignoreWater.get() && (mc.player.isSubmergedInWater())) return false;

        float speed = mc.player.forwardSpeed;
        if (allDirections.get()) {
            speed = Math.abs(speed);
            speed += Math.abs(mc.player.sidewaysSpeed);
        }
        return allDirections.get() ? speed != 0 : speed > 0;
    }
}
