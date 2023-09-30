package bananaplus.fixedmodules.combat;

import bananaplus.BananaPlus;
import bananaplus.fixedutils.CombatUtil;
import bananaplus.system.BananaConfig;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

public class PhaseESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Only render players within this range.")
        .defaultValue(10.0)
        .sliderRange(0,20)
        .build()
    );

    private final Setting<Boolean> renderWebbed = sgGeneral.add(new BoolSetting.Builder()
        .name("show-webbed")
        .description("Render players that are webbed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("render-self")
        .description("Render your own burrow block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("render-friends")
        .description("Render players you have added.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>()
        .name("render-mode")
        .description("How to render burrowed players.")
        .defaultValue(RenderMode.Hitbox)
        .build()
    );

    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder()
        .name("text-color")
        .description("The color of the text.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .visible(() -> renderMode.get() != RenderMode.Hitbox)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Shape mode of the hitbox render.")
        .defaultValue(ShapeMode.Both)
        .visible(() -> renderMode.get() != RenderMode.Text)
        .build()
    );

    private final Setting<SettingColor> phasedSides = sgRender.add(new ColorSetting.Builder()
        .name("phased-sides")
        .description("The side color for phased players.")
        .defaultValue(new SettingColor(255, 30, 180, 25))
        .visible(() -> renderMode.get() != RenderMode.Text && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> phasedLines = sgRender.add(new ColorSetting.Builder()
        .name("phased-lines")
        .description("The line color for phased players.")
        .defaultValue(new SettingColor(255, 30, 180, 255))
        .visible(() -> renderMode.get() != RenderMode.Text && shapeMode.get().lines())
        .build()
    );

    private final Setting<SettingColor> webbedSides = sgRender.add(new ColorSetting.Builder()
        .name("webbed-sides")
        .description("The side color for webbed players.")
        .defaultValue(new SettingColor(255, 255, 255, 25))
        .visible(() -> renderMode.get() != RenderMode.Text && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> webbedLines = sgRender.add(new ColorSetting.Builder()
        .name("webbed-lines")
        .description("The line color for webbed players.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .visible(() -> renderMode.get() != RenderMode.Text && shapeMode.get().lines())
        .build()
    );


    public PhaseESP() {
        super(BananaPlus.FIXED, "phase-ESP", "Displays players that are phased into blocks .");
    }

    public Map<PlayerEntity, Boolean> players = new HashMap<>();
    private final Vector3d pos = new Vector3d();

    @EventHandler
    private void onTick(TickEvent.Post event) {
        players.clear();

        for (PlayerEntity player: mc.world.getPlayers()) {
            if (player == mc.player && !renderSelf.get()) continue;
            if (Friends.get().isFriend(player) && !renderFriends.get()) continue;
            if (mc.gameRenderer.getCamera().getPos().distanceTo(player.getPos()) > range.get()) continue;

            if (CombatUtil.isWebbed(player)) players.put(player, true);
            else if (CombatUtil.isPhased(player)) players.put(player, false);
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (players.isEmpty() || renderMode.get() == RenderMode.Text) return;

        players.forEach((player, webbed) -> {
            double x = MathHelper.lerp(event.tickDelta, player.lastRenderX, player.getX()) - player.getX();
            double y = MathHelper.lerp(event.tickDelta, player.lastRenderY, player.getY()) - player.getY();
            double z = MathHelper.lerp(event.tickDelta, player.lastRenderZ, player.getZ()) - player.getZ();
            Box box = player.getBoundingBox().offset(x, y, z);

            event.renderer.box(
                box, (webbed) ? webbedSides.get() : phasedSides.get(),
                (webbed) ? webbedLines.get() : phasedLines.get(),
                shapeMode.get(), 0
            );
        });
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (players.isEmpty() || renderMode.get() == RenderMode.Hitbox) return;

        boolean shadow = Config.get().customFont.get();

        players.forEach((player, isWebbed) -> {
            Utils.set(pos, player, event.tickDelta).add(0, 0.5, 0);
            double scale = BananaConfig.get().getScale(pos);

            if (NametagUtils.to2D(pos, scale)) {
                NametagUtils.begin(pos);
                TextRenderer.get().begin(1.0, false, true);

                String text = (isWebbed) ? "Webbed" : "Burrowed";
                double width = TextRenderer.get().getWidth(text) / 2.0;
                TextRenderer.get().render(text, -width, 0.0, textColor.get(), shadow);

                TextRenderer.get().end();
                NametagUtils.end();
            }
        });
    }

    public enum RenderMode {
        Hitbox,
        Text,
        Both
    }
}
