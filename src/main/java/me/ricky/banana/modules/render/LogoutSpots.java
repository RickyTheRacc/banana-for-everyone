package me.ricky.banana.modules.render;

import me.ricky.banana.BananaPlus;
import me.ricky.banana.events.JoinEvent;
import me.ricky.banana.events.LeaveEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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

    public final Setting<NotifyMode> logoutMode = sgGeneral.add(new EnumSetting.Builder<NotifyMode>()
        .name("logouts")
        .description("How to notify you when someone logs out.")
        .defaultValue(NotifyMode.Both)
        .build()
    );

    public final Setting<NotifyMode> rejoinMode = sgGeneral.add(new EnumSetting.Builder<NotifyMode>()
        .name("rejoins")
        .description("How to notify you when someone rejoins.")
        .defaultValue(NotifyMode.Both)
        .build()
    );

    private final Setting<Boolean> culling = sgGeneral.add(new BoolSetting.Builder()
        .name("culling")
        .description("Render a certain number of logout spots within a certain distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxCount = sgGeneral.add(new IntSetting.Builder()
        .name("max-count")
        .description("Only render this many nametags.")
        .defaultValue(50)
        .min(1)
        .sliderRange(1, 100)
        .visible(culling::get)
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
        super(BananaPlus.CATEGORY, "logout-spots", "Shows where players logged out.");
    }

    private final List<LogoutSpot> logoutSpots = new ArrayList<>();
    private final List<PlayerEntity> lastPlayers = new ArrayList<>();

    private static final Color GREEN = new Color(25, 225, 25);
    private static final Color ORANGE = new Color(225, 105, 25);
    private static final Color RED = new Color(225, 25, 25);
    private static final Vector3d pos = new Vector3d();

    private final SoundInstance sound = PositionedSoundInstance.master(
        SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.2f, 1
    );

    @Override
    public void onActivate() {
        if (!Utils.canUpdate()) return;
        updateLastPlayers();
    }

    @Override
    public void onDeactivate() {
        logoutSpots.clear();
        lastPlayers.clear();
    }

    @EventHandler
    public void onGameJoin(GameJoinedEvent event) {
        logoutSpots.clear();
        lastPlayers.clear();
        updateLastPlayers();
    }

    @EventHandler
    private void onPlayerLeave(LeaveEvent event) {
        if (onlyTargets.get() && !event.wasTarget) return;

        for (PlayerEntity player : lastPlayers) {
            if (!player.getUuid().equals(event.entry.getProfile().getId())) continue;
            if (Friends.get().isFriend(player) && ignoreFriends.get()) continue;

            LogoutSpot logSpot = new LogoutSpot(player);
            logoutSpots.removeIf(logoutSpot -> logoutSpot.uuid.equals(logSpot.uuid) );
            logoutSpots.add(logSpot);

            if (culling.get() && logoutSpots.size() > maxCount.get()) logoutSpots.removeFirst();

            if (logoutMode.get().chat()) {
                warning("%s logged out at (%d, %d, %d) in the %s.",
                    logSpot.player.getName().getString(),
                    logSpot.player.getBlockX(),
                    logSpot.player.getBlockY(),
                    logSpot.player.getBlockZ(),
                    logSpot.dimension.toString()
                );
            }
            if (logoutMode.get().sound()) mc.getSoundManager().play(sound);

            break;
        }
    }

    @EventHandler
    private void onPlayerJoin(JoinEvent event) {
        Iterator<LogoutSpot> iterator = logoutSpots.iterator();
        while (iterator.hasNext()) {
            LogoutSpot logSpot = iterator.next();
            if (!logSpot.uuid.equals(event.entry.getProfile().getId())) continue;

            if (rejoinMode.get().chat()) {
                warning("%s logged back in at (%d, %d, %d) in the %s.",
                    logSpot.player.getName().getString(),
                    logSpot.player.getBlockX(),
                    logSpot.player.getBlockY(),
                    logSpot.player.getBlockZ(),
                    logSpot.dimension.toString()
                );
            }
            if (rejoinMode.get().sound()) mc.getSoundManager().play(sound);

            iterator.remove();
            break;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        updateLastPlayers();
    }

    private void updateLastPlayers() {
        lastPlayers.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (EntityUtils.getGameMode(player) == null) continue;
            if (player == mc.player) continue;
            lastPlayers.add(player);
        }
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
            tickDelta = MeteorClient.mc.getTickDelta();

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
        Chat,
        Sound,
        None;

        public boolean chat() {
            return this == Both || this == Chat;
        }

        public boolean sound() {
            return this == Both || this == Sound;
        }
    }
}