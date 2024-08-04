package me.ricky.banana.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

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

    public final Setting <Boolean> ignoreHunger = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-hunger")
        .description("Sprint even when below 6 hunger.")
        .defaultValue(false)
        .build()
    );

    public final Setting <Boolean> ignoreWater = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-water")
        .description("Try to sprint even when touching water.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> preserveCrits = sgGeneral.add(new BoolSetting.Builder()
        .name("preserve-crits")
        .description("Stop sprinting momentarily when attacking entities, doesn't work when on ground.")
        .defaultValue(false)
        .build()
    );

    private final GUIMove guiMove = Modules.get().get(GUIMove.class);

    public Sprint() {
        super(Categories.Movement, "sprint-2", "Automatically sprints.");
    }

    @EventHandler(priority = 500)
    private void onTick(TickEvent.Post event) {
        if (shouldAutoSprint()) mc.player.setSprinting(true);
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!preserveCrits.get() || !mc.player.isSprinting()) return;

        if (event.packet instanceof IPlayerInteractEntityC2SPacket packet) {
            if (packet.getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            mc.player.setSprinting(false);
        }
    }

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        if (!preserveCrits.get() || !shouldAutoSprint() || mc.player.isSprinting()) return;

        if (event.packet instanceof IPlayerInteractEntityC2SPacket packet) {
            if (packet.getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            mc.player.setSprinting(true);
        }
    }

    public boolean shouldAutoSprint() {
        if (!isActive()) return false;

        if (!ignoreHunger.get() && mc.player.getHungerManager().getFoodLevel() <= 6) return false;
        if (!ignoreWater.get() && (mc.player.isSubmergedInWater() || mc.player.isTouchingWater())) return false;
        if (!preventStop.get() && mc.player.horizontalCollision && !mc.player.collidedSoftly) return false;

        if (mc.currentScreen != null && (!guiMove.isActive() || !guiMove.sprint.get())) return false;

        float speed = mc.player.forwardSpeed;
        if (allDirections.get()) {
            speed = Math.abs(speed);
            speed += Math.abs(mc.player.sidewaysSpeed);
        }

        return allDirections.get() ? speed != 0 : speed > 0;
    }
}
