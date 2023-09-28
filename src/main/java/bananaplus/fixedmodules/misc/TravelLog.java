package bananaplus.fixedmodules.misc;

import bananaplus.BananaPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.Text;
import org.joml.Vector3d;

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

    private final Setting<Boolean> toggleAutoReconnect = sgGeneral.add(new BoolSetting.Builder()
            .name("prevent-reconnect")
            .description("Turns off auto reconnect when disconnecting.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-toggle")
            .description("Turns itself off when disconnecting.")
            .defaultValue(true)
            .build()
    );

    public TravelLog() {
        super(BananaPlus.FIXED, "travel-log", "Logs out when you're near certain coords. Useful for AFKing with Baritone.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (PlayerUtils.getDimension() != dimension.get()) return;
        if (coords.get().distance(mc.player.getX(), mc.player.getY(), mc.player.getZ()) > radius.get()) return;

        if (toggleAutoReconnect.get() && Modules.get().isActive(AutoReconnect.class)) Modules.get().get(AutoReconnect.class).toggle();
        if (autoToggle.get()) toggle();

        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("Arrived near target destination.")));
    }
}
