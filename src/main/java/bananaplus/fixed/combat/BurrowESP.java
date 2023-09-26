package bananaplus.fixed.combat;

import bananaplus.BananaPlus;
import bananaplus.utils.BEntityUtils;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;

public class BurrowESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgText = settings.createGroup("Text");
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

    private final Setting<RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
        .name("render-mode")
        .description("Render text in the middle of a player's burrow block.")
        .defaultValue(RenderMode.Text)
        .build()
    );

    // Text



    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> renderMode.get().block())
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("normal-sides")
            .description("The side color of the rendering.")
            .defaultValue(new SettingColor(230, 0, 255, 25))
            .visible(() -> shapeMode.get().sides() && renderMode.get().block())
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("normal-lines")
            .description("The line color of the rendering.")
            .defaultValue(new SettingColor(230, 0, 255, 255))
            .visible(() -> shapeMode.get().lines() && renderMode.get().block())
            .build()
    );

    private final Setting<SettingColor> webSideColor = sgRender.add(new ColorSetting.Builder()
            .name("webbed-sides")
            .description("The side color of the rendering for webs.")
            .defaultValue(new SettingColor(255, 255, 255, 25))
            .visible(() -> shapeMode.get().sides() && renderWebbed.get() && renderMode.get().block())
            .build()
    );

    private final Setting<SettingColor> webLineColor = sgRender.add(new ColorSetting.Builder()
            .name("webbed-lines")
            .description("The line color of the rendering for webs.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> shapeMode.get().lines() && renderWebbed.get() && renderMode.get().block())
            .build()
    );

    public BurrowESP() {
        super(BananaPlus.FIXED, "Burrow-ESP", "Displays players that are burrowed or webbed.");
    }

    public Map<PlayerEntity, Boolean> players = new HashMap<>();

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (PlayerEntity player: mc.world.getPlayers()) {
            if (player == mc.player && !renderSelf.get()) continue;
            if (Friends.get().isFriend(player) && !renderFriends.get()) continue;
            if (mc.player.getEyePos().distanceTo(player.getPos()) > range.get()) continue;

            if (BEntityUtils.isWebbed(player) && renderWebbed.get()) players.put(player, true);
            if (BEntityUtils.isBurrowed(player, BEntityUtils.BlastResistantType.Any)) players.put(player, false);
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderMode.get().block() || players.isEmpty()) return;

        players.forEach((player, isWebbed) -> {
            if (!isWebbed) event.renderer.box(player.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            else event.renderer.box(player.getBlockPos(), webSideColor.get(), webLineColor.get(), shapeMode.get(), 0);
        });
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!renderMode.get().text() || players.isEmpty()) return;

    }
    
    public enum RenderMode {
        Block,
        Text,
        Both;
        
        public boolean block() {
            return this == Block || this == Both;
        }
        
        public boolean text() {
            return this == Text || this == Both;
        }
    }
}
