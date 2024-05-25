package me.ricky.banana.modules;

import com.google.common.base.Stopwatch;
import me.ricky.banana.BananaPlus;
import me.ricky.banana.systems.BananaModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Breadcrumbs;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class Blink extends BananaModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Boolean> limitRange = sgGeneral.add(new BoolSetting.Builder()
        .name("limit-range")
        .description("Toggle after travelling a certain distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> maxRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-range")
        .description("The maximum distance you can travel while blinking.")
        .defaultValue(15)
        .range(1, 45)
        .sliderRange(1, 45)
        .visible(limitRange::get)
        .build()
    );

    private final Setting<Boolean> limitTime = sgGeneral.add(new BoolSetting.Builder()
        .name("limit-time")
        .description("Toggle after you've been blinking for a certain number of seconds.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> maxTime = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-time")
        .description("The maximum time you can blink.")
        .defaultValue(5.0)
        .range(1.0, 10.0)
        .sliderMin(1)
        .visible(limitTime::get)
        .build()
    );

    private final Setting<Keybind> cancelBlink = sgGeneral.add(new KeybindSetting.Builder()
        .name("cancel-bind")
        .description("Cancels sending packets and sends you back to your original position.")
        .defaultValue(Keybind.none())
        .action(() -> {
            canceled = true;
            if (isActive()) toggle();
        })
        .build()
    );

    // Render

    private final Setting<Boolean> useBreadcrumbs = sgRender.add(new BoolSetting.Builder()
        .name("breadcrumbs")
        .description("Automatically turn on the breadcrumbs module to show your blink path.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderStart = sgRender.add(new BoolSetting.Builder()
        .name("render-start")
        .description("Render your player model at the original position.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(renderStart::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 255, 255, 25))
        .visible(renderStart::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 255, 255, 127))
        .visible(renderStart::get)
        .build()
    );

    public Blink() {
        super(Categories.Render, "blink", "Cancel movement packets to move in the blink of an eye.");
    }

    private final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
    public final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private FakePlayerEntity model;
    private boolean canceled;
    private float prevPitch, prevYaw;

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private final Box hitbox = new Box(BlockPos.ORIGIN);
    private final Vec3d eyePos = new Vec3d(0, 0, 0);
    private final Vec3d feetPos = new Vec3d(0, 0, 0);

    @Override
    public void onActivate() {
        blockPos.set(
            mc.player.getBlockX(),
            mc.player.getBlockY(),
            mc.player.getBlockZ()
        );

        ((IBox) hitbox).set(
            mc.player.getBoundingBox().minX,
            mc.player.getBoundingBox().minY,
            mc.player.getBoundingBox().minZ,
            mc.player.getBoundingBox().maxX,
            mc.player.getBoundingBox().maxY,
            mc.player.getBoundingBox().maxZ
        );

        ((IVec3d) eyePos).set(
            mc.player.getEyePos().x,
            mc.player.getEyePos().y,
            mc.player.getEyePos().z
        );

        ((IVec3d) feetPos).set(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ()
        );

        prevPitch = mc.player.getPitch();
        prevYaw = mc.player.getYaw();

        Breadcrumbs breadcrumbs = Modules.get().get(Breadcrumbs.class);
        if (useBreadcrumbs.get() && !breadcrumbs.isActive()) breadcrumbs.toggle();
        model = new FakePlayerEntity(mc.player, "ghost", 20, false);

        stopwatch.start();
        canceled = false;
    }

    @Override
    public void onDeactivate() {
        Breadcrumbs breadcrumbs = Modules.get().get(Breadcrumbs.class);
        if (useBreadcrumbs.get() && breadcrumbs.isActive()) breadcrumbs.toggle();
        if (renderStart.get()) model = null;

        stopwatch.stop();
        stopwatch.reset();

        synchronized (packets) {
            if (canceled) {
                mc.player.setPos(feetPos.x, feetPos.y, feetPos.z);
                mc.player.setPitch(prevPitch);
                mc.player.setYaw(prevYaw);
            }
            else packets.forEach(mc.player.networkHandler::sendPacket);

            packets.clear();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (limitRange.get() && mc.player.getPos().distanceTo(feetPos) >= maxRange.get()) {
            info("Traveled %s blocks, toggling.", maxRange.get().intValue());
            toggle();
            return;
        }

        if (limitTime.get() && stopwatch.elapsed(TimeUnit.SECONDS) >= maxTime.get()) {
            info("Blinked for %s seconds, toggling.", maxTime.get());
            toggle();
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerMoveC2SPacket packet)) return;
        event.cancel();

        if (packets.isEmpty()) return;
        PlayerMoveC2SPacket prev = packets.getLast();

        if (packet.isOnGround() == prev.isOnGround() &&
            packet.getYaw(-1) == prev.getYaw(-1) &&
            packet.getPitch(-1) == prev.getPitch(-1) &&
            packet.getX(-1) == prev.getX(-1) &&
            packet.getY(-1) == prev.getY(-1) &&
            packet.getZ(-1) == prev.getZ(-1)
        ) return;

        synchronized (packets) { packets.add(packet); }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (model == null || !renderStart.get()) return;
        WireframeEntityRenderer.render(event, model, 1.0, sideColor.get(), lineColor.get(), shapeMode.get());
    }

    @Override
    public String getInfoString() {
        double multiplier = Modules.get().get(Timer.class).getMultiplier();
        return String.format("%.3f", (float) (stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000) * multiplier);
    }

    public PlayerEntity realPlayer() {
        return model;
    }

    public BlockPos realBlockPos() {
        // Since sometimes with things like standing on echests or pearling
        // the pos is technically like 0.1 below our actual feet level pos

        return BlockPos.ofFloored(
            blockPos.getX(),
            blockPos.getY() + 0.4,
            blockPos.getZ()
        );
    }

    public Box realHitbox() {
        return hitbox;
    }

    public Vec3d realEyesPos() {
        return eyePos;
    }

    public Vec3d realFeetPos() {
        return feetPos;
    }
}
