package bananaplus.modules.misc;

import bananaplus.modules.AddModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.util.ArrayList;
import java.util.List;

public class NecroSimulator extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> disableOnRubberband = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-rubberband")
            .description("Automatically disables on rubberband")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only pulses if you are on the ground.")
            .defaultValue(false)
            .build());

    private final Setting<Double> pulseTimer = sgGeneral.add(new DoubleSetting.Builder()
            .name("interval")
            .description("How many seconds between each pulse time.")
            .defaultValue(1)
            .sliderRange(0,15)
            .build());

    public NecroSimulator() {
        super(AddModule.MISC, "necro-simulator", "Makes you very laggy server side.");
    }

    private List<PlayerMoveC2SPacket> queue = new ArrayList<>();
    private long startTime = 0;
    private boolean rubberbanded;

    @Override
    public void onActivate() {
        rubberbanded = false;
        startTime = System.currentTimeMillis();
        queue.clear();
    }

    @Override
    public void onDeactivate() {
        if (Utils.canUpdate()) sendPackets();
    }

    @EventHandler
    public void sendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            queue.add((PlayerMoveC2SPacket) event.packet);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (System.currentTimeMillis() - startTime > pulseTimer.get() * 1000 && (onlyOnGround.get() && mc.player.isOnGround() || !onlyOnGround.get())) {
            toggle();
            toggle();
        }

        if (disableOnRubberband.get() && rubberbanded && isActive()) {
            info("Rubberband detected, disabling.");
            toggle();
        }
    }

    public void sendPackets() {
        for (PlayerMoveC2SPacket p : new ArrayList<>(queue)) {
            if (!(p instanceof PlayerMoveC2SPacket.LookAndOnGround)) {
                mc.player.networkHandler.sendPacket(p);
            }
        }

        queue.clear();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
           rubberbanded = true;
        }
    }
}
