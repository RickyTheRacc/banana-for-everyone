package me.ricky.banana.modules.movement;

import me.ricky.banana.BananaPlus;
import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Sprint extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    public final Setting<SprintMode> sprintMode = sgGeneral.add(new EnumSetting.Builder<SprintMode>()
        .name("sprint-mode")
        .description("How sprinting should behave.")
        .defaultValue(SprintMode.Vanilla)
        .build()
    );

    public final Setting <Boolean> allDirections = sgGeneral.add(new BoolSetting.Builder()
        .name("all-directions")
        .description("Allows you to sprint sideways and backwards.")
        .defaultValue(false)
        .build()
    );

    public final Setting <Boolean> ignoreLiquids = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-liquids")
        .description("Try to sprint even when in water/lava.")
        .defaultValue(true)
        .build()
    );

    public final Setting <Boolean> ignoreHunger = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-hunger")
        .description("Sprint even when below 6 hunger.")
        .defaultValue(true)
        .build()
    );

    public final Setting <Boolean> cancelSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-sprint")
        .description("Stop you from sprinting when the module is disabled.")
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
        if (mc.player.getHungerManager().getFoodLevel() <= 6 && !ignoreHunger.get()) return false;
        if (ignoreLiquids.get() && (mc.player.isSubmergedInWater() || mc.player.isInLava())) return false;

        boolean moving = mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0 && allDirections.get();
        return moving || sprintMode.get() == SprintMode.Blatant;
    }

    @Override
    public void onDeactivate() {
        if (!cancelSprint.get()) return;
        mc.player.setSprinting(false);
        mc.player.jump();
    }

    public enum SprintMode {
        Vanilla,
        Blatant
    }
}
