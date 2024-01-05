package me.ricky.banana.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Sprint extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    public final Setting<SprintMode> sprintMode = sgGeneral.add(new EnumSetting.Builder<SprintMode>()
        .name("sprint-mode")
        .description("How sprinting should behave.")
        .defaultValue(SprintMode.Vanilla)
        .build()
    );

    public final Setting <Boolean> ignoreHunger = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-hunger")
        .description("Sprint even when below 6 hunger.")
        .defaultValue(false)
        .build()
    );

    public final Setting <Boolean> cancelSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-sprint")
        .description("Stop you from sprinting when the module is disabled.")
        .defaultValue(false)
        .build()
    );

    public Sprint() {
        super(Categories.Movement, "sprint", "Automatically sprints.");
    }

    @EventHandler(priority = 500)
    private void onTick(TickEvent.Post event) {
        if (mc.player.getHungerManager().getFoodLevel() <= 6 && !ignoreHunger.get()) {
            mc.player.setSprinting(false);
            return;
        }

        if (!mc.options.forwardKey.isPressed() && sprintMode.get() == SprintMode.Vanilla) return;

        mc.player.setSprinting(true);
    }

    @Override
    public void onDeactivate() {
        if (!cancelSprint.get()) return;
        mc.player.setSprinting(false);
    }

    public enum SprintMode {
        Vanilla,
        Blatant
    }
}
