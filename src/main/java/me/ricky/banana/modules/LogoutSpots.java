package me.ricky.banana.modules;

import me.ricky.banana.BananaPlus;
import me.ricky.banana.events.JoinEvent;
import me.ricky.banana.events.LeaveEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
public class LogoutSpots extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Boolean> onlyTargets = sgGeneral.add(new BoolSetting.Builder()
        .name("only-targets")
        .description("Only record players who logged in combat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Don't log your friend's logout spots.")
        .defaultValue(true)
        .visible(() -> !onlyTargets.get())
        .build()
    );

    public final Setting<NotifyMode> chatMode = sgGeneral.add(new EnumSetting.Builder<NotifyMode>()
        .name("chat-mode")
        .description("Send a chat message when someone joins/leaves.")
        .defaultValue(NotifyMode.Both)
        .build()
    );

    public final Setting<NotifyMode> soundMode = sgGeneral.add(new EnumSetting.Builder<NotifyMode>()
        .name("sound-mode")
        .description("Play ding sounds if someone joins/leaves.")
        .defaultValue(NotifyMode.Both)
        .build()
    );

    private final Setting<Integer> volume = sgGeneral.add(new IntSetting.Builder()
        .name("volume")
        .description("The volume of the sound played.")
        .defaultValue(50)
        .min(1)
        .sliderRange(1,100)
        .visible(() -> soundMode.get() != NotifyMode.None)
        .build()
    );

    // Render

    private final Setting<Double> scale = sgRender.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("The scale of the nametag.")
        .defaultValue(1.0)
        .min(0)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 0, 255, 55))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 0, 255))
        .build()
    );

    private final Setting<SettingColor> nameBackgroundColor = sgRender.add(new ColorSetting.Builder()
        .name("background-color")
        .description("The background color of the nametag.")
        .defaultValue(new SettingColor(0, 0, 0, 75))
        .build()
    );

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("name-color")
        .description("The name color in the nametag.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    public LogoutSpots() {
        super(BananaPlus.CATEGORY, "b+-logout-spots", "Shows where players logged out.");
    }

    private final List<LogoutSpot> logoutSpots = new ArrayList<>();

    private static final Color GREEN = new Color(25, 225, 25);
    private static final Color ORANGE = new Color(225, 105, 25);
    private static final Color RED = new Color(225, 25, 25);
    private static final Vector3d pos = new Vector3d();

    @Override
    public void onDeactivate() {
        logoutSpots.clear();
    }

    @EventHandler
    public void onGameJoin(GameJoinedEvent event) {
        logoutSpots.clear();
    }

    @EventHandler
    private void onPlayerLeave(LeaveEvent event) {
        if (event.player == null || (onlyTargets.get() && !event.wasTarget)) return;
        if (Friends.get().isFriend(event.player) && ignoreFriends.get()) return;

        LogoutSpot logSpot = new LogoutSpot(event.player);
        logoutSpots.removeIf(spot -> spot.uuid.equals(event.player.getUuid()));
        logoutSpots.add(logSpot);

        if (chatMode.get().leaves()) {
            warning("%s logged out at (%d, %d, %d) in the %s.",
                logSpot.player.getName().getString(),
                logSpot.player.getBlockX(),
                logSpot.player.getBlockY(),
                logSpot.player.getBlockZ(),
                logSpot.dimension.toString()
            );
        }

        if (soundMode.get().leaves()) mc.getSoundManager().play(PositionedSoundInstance.master(
            SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.2f, volume.get()
        ));
    }

    @EventHandler
    private void onPlayerJoin(JoinEvent event) {
        LogoutSpot logSpot = logoutSpots.stream().filter(spot ->
            spot.uuid.equals(event.entry.getProfile().getId())
        ).findFirst().orElse(null);

        if (logSpot == null) return;
        logoutSpots.remove(logSpot);

        if (chatMode.get().joins()) {
            warning("%s logged back in at (%d, %d, %d) in the %s.",
                logSpot.player.getName().getString(),
                logSpot.player.getBlockX(),
                logSpot.player.getBlockY(),
                logSpot.player.getBlockZ(),
                logSpot.dimension.toString()
            );
        }

        if (soundMode.get().joins()) mc.getSoundManager().play(PositionedSoundInstance.master(
            SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.2f, volume.get()
        ));
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        Dimension currentDimension = PlayerUtils.getDimension();
        float originalDelta = event.tickDelta;

        for (LogoutSpot logoutSpot: logoutSpots) {
            if (logoutSpot.dimension != currentDimension) return;
            if (!PlayerUtils.isWithinCamera(logoutSpot.player.getPos(), mc.options.getViewDistance().getValue() * 16)) return;

            event.tickDelta = logoutSpot.tickDelta;
            WireframeEntityRenderer.render(event, logoutSpot.player, 1.0, sideColor.get(), lineColor.get(), this.shapeMode.get());
        }

        event.tickDelta = originalDelta;
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        Dimension currentDimension = PlayerUtils.getDimension();
        for (LogoutSpot logoutSpot : logoutSpots) {
            if (logoutSpot.dimension != currentDimension) return;
            if (!PlayerUtils.isWithinCamera(logoutSpot.player.getPos(), mc.options.getViewDistance().getValue() * 16)) return;

            double scale = this.scale.get();
            pos.set(
                logoutSpot.player.getBoundingBox().getCenter().x ,
                logoutSpot.player.getBoundingBox().maxY + 0.5,
                logoutSpot.player.getBoundingBox().getCenter().z
            );
            if (!NametagUtils.to2D(pos, scale)) return;

            TextRenderer text = TextRenderer.get();
            NametagUtils.begin(pos);

            // Render background
            double i = text.getWidth(logoutSpot.name) / 2.0 + text.getWidth(" " + logoutSpot.health) / 2.0;
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-i, 0, i * 2, text.getHeight(), nameBackgroundColor.get());
            Renderer2D.COLOR.render(null);

            // Render name and health texts
            text.beginBig();
            double hX = text.render(logoutSpot.name, -i, 0, nameColor.get());
            text.render(" " + logoutSpot.health, hX, 0, logoutSpot.color);
            text.end();

            NametagUtils.end();
        }
    }

    @Override
    public String getInfoString() {
        return Integer.toString(logoutSpots.size());
    }

    private static class LogoutSpot {
        public final Dimension dimension;
        public final PlayerEntity player;
        public final float tickDelta;

        public final UUID uuid;
        public final String name;
        public final int health;
        public final Color color;

        public LogoutSpot(PlayerEntity player) {
            dimension = PlayerUtils.getDimension();
            this.player = player;
            tickDelta = MeteorClient.mc.getRenderTickCounter().getTickDelta(false);

            uuid = player.getUuid();
            name = player.getName().getString();
            health = Math.round(player.getHealth() + player.getAbsorptionAmount());

            int maxHealth = Math.round(player.getMaxHealth() + player.getAbsorptionAmount());
            double healthPercentage = (double) health / maxHealth;

            if (healthPercentage <= 0.333) color = RED;
            else if (healthPercentage <= 0.666) color = ORANGE;
            else color = GREEN;
        }
    }

    public enum NotifyMode {
        Both,
        Joins,
        Leaves,
        None;

        public boolean joins() {
            return this == Both || this == Joins;
        }

        public boolean leaves() {
            return this == Both || this == Leaves;
        }
    }
}