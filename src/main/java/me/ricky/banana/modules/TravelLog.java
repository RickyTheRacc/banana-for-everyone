package me.ricky.banana.modules;

import me.ricky.banana.BananaPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import org.joml.Vector3d;

@SuppressWarnings("ConstantConditions")
public class TravelLog extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Dimension> dimension = sgGeneral.add(new EnumSetting.Builder<Dimension>()
        .name("dimension")
        .description("Dimension for the coords.")
        .defaultValue(Dimension.Nether)
        .build()
    );

    private final Setting<Vector3d> coords = sgGeneral.add(new Vector3dSetting.Builder()
        .name("coordinates")
        .description("What coordinates to logout near to.")
        .defaultValue(0, 0, 0)
        .decimalPlaces(0)
        .noSlider()
        .build()
    );

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("distance")
        .description("How many blocks away from the point to log you out.")
        .defaultValue(500)
        .sliderRange(0,1000)
        .build()
    );

    public TravelLog() {
        super(BananaPlus.CATEGORY, "travel-log", "Logs out when you're near certain coords.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (PlayerUtils.getDimension() != dimension.get()) return;
        if (coords.get().distance(mc.player.getX(), mc.player.getY(), mc.player.getZ()) > radius.get()) return;

        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
        if (autoReconnect.isActive()) autoReconnect.toggle();

        toggle();

        mc.getNetworkHandler().onDisconnect(new DisconnectS2CPacket(Text.literal("Arrived near target destination.")));
    }
}