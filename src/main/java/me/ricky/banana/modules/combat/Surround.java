package me.ricky.banana.modules.combat;

import me.ricky.banana.BananaPlus;
import me.ricky.banana.utils.BlockUtil;
import me.ricky.banana.utils.DynamicUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.*;

public class Surround extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAntiCheat = settings.createGroup("Anticheat");
    private final SettingGroup sgCrystals = settings.createGroup("Crystals");
    private final SettingGroup sgToggles = settings.createGroup("Toggles");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<List<Block>> primaryBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("primary-blocks")
        .description("What blocks to use for Surround.")
        .defaultValue(Blocks.OBSIDIAN)
        .filter(BlockUtil::combatFilter)
        .build()
    );

    private final Setting<List<Block>> backupBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("backup-blocks")
        .description("What blocks to use if no primary blocks are found.")
        .defaultValue(Blocks.ENDER_CHEST)
        .filter(BlockUtil::combatFilter)
        .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("center")
        .description("Centers the player over a block when surround is incomplete.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder()
        .name("on-ground")
        .description("Only try to place blocks when you're on the ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlySneak = sgGeneral.add(new BoolSetting.Builder()
        .name("on-sneak")
        .description("Only try to place blocks when you're sneaking.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> notifyBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("notify-break")
        .description("Notify you if someone starts breaking your surround.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> notifyToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("notify-toggle")
        .description("Notify you when the module toggles off.")
        .defaultValue(false)
        .build()
    );

    // Toggles

    private final Setting<Boolean> toggleOnYChange = sgToggles.add(new BoolSetting.Builder()
        .name("toggle-on-y-change")
        .description("Automatically disables when your Y level changes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleOnComplete = sgToggles.add(new BoolSetting.Builder()
        .name("toggle-on-complete")
        .description("Toggles off when all blocks are placed.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnTeleport = sgToggles.add(new BoolSetting.Builder()
        .name("toggle-on-teleport")
        .description("Toggles off when you use a chorus fruit or pearl.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swing your hand as you place blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> renderPlace = sgRender.add(new BoolSetting.Builder()
        .name("render-place")
        .description("Will render where surround is placing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<ShapeMode> placeShapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered for placing.")
        .defaultValue(ShapeMode.Lines)
        .visible(renderPlace::get)
        .build()
    );

    private final Setting<SettingColor> placeSides = sgRender.add(new ColorSetting.Builder()
        .name("place-side-color")
        .description("The color of placing blocks.")
        .defaultValue(new SettingColor(255, 255, 255, 25))
        .visible(() -> renderPlace.get() && placeShapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> placeLines = sgRender.add(new ColorSetting.Builder()
        .name("place-line-color")
        .description("The color of placing line.")
        .defaultValue(new SettingColor(255, 255, 255, 150))
        .visible(() -> renderPlace.get() && placeShapeMode.get().lines())
        .build()
    );

    private final Setting<Boolean> renderActive = sgRender.add(new BoolSetting.Builder()
        .name("render-active")
        .description("Will render which blocks surround is currently protecting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> activeShapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered for active blocks.")
        .defaultValue(ShapeMode.Lines)
        .visible(renderActive::get)
        .build()
    );

    private final Setting<SettingColor> safeSides = sgRender.add(new ColorSetting.Builder()
        .name("safe-sides")
        .description("The side color for safe blocks.")
        .defaultValue(new SettingColor(13, 255, 0, 15))
        .visible(() -> renderActive.get() && activeShapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> safeLines = sgRender.add(new ColorSetting.Builder()
        .name("safe-lines")
        .description("The line color for safe blocks.")
        .defaultValue(new SettingColor(13, 255, 0, 125))
        .visible(() -> renderActive.get() && activeShapeMode.get().lines())
        .build()
    );

    private final Setting<SettingColor> normalSides = sgRender.add(new ColorSetting.Builder()
        .name("normal-sides")
        .description("The side color for normal blocks.")
        .defaultValue(new SettingColor(0, 255, 238, 15))
        .visible(() -> renderActive.get() && activeShapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> normalLines = sgRender.add(new ColorSetting.Builder()
        .name("normal-lines")
        .description("The line color for normal blocks.")
        .defaultValue(new SettingColor(0, 255, 238, 125))
        .visible(() -> renderActive.get() && activeShapeMode.get().lines())
        .build()
    );

    private final Setting<SettingColor> unsafeSides = sgRender.add(new ColorSetting.Builder()
        .name("unsafe-sides")
        .description("The side color for unsafe blocks.")
        .defaultValue(new SettingColor(204, 0, 0, 15))
        .visible(() -> renderActive.get() && activeShapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> unsafeLines = sgRender.add(new ColorSetting.Builder()
        .name("unsafe-lines")
        .description("The line color for unsafe blocks.")
        .defaultValue(new SettingColor(204, 0, 0, 125))
        .visible(() -> renderActive.get() && activeShapeMode.get().lines())
        .build()
    );

    public Surround() {
        super(BananaPlus.FIXED, "surround-plus", "guh?");
    }

    private final List<BlockPos> positions = new ArrayList<>();
    private final List<BlockPos> breaking = new ArrayList<>();
    private final Blink blink = Modules.get().get(Blink.class);

    private int blockDelay;
    private int crystalDelay;

    @Override
    public void onActivate() {
        positions.clear();
        breaking.clear();

        blockDelay = 0;
        crystalDelay = 0;

        if (center.get() && !blink.isActive()) {
            PlayerUtils.centerPlayer();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (toggleOnYChange.get() && mc.player.getY() != mc.player.prevY && !blink.isActive()) {
            if (notifyToggle.get()) error("Toggled off because your Y changed.");
            toggle();
            return;
        }

        positions.clear();
        positions.addAll(DynamicUtil.feetPos(mc.player));
        positions.addAll(DynamicUtil.underPos(mc.player));
        positions.sort(Comparator.comparingInt(Vec3i::getY).reversed());

        if (!blink.isActive()) {
            if (onlyGround.get() && !mc.player.isOnGround()) return;
            if (onlySneak.get() && !mc.player.isSneaking()) return;
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
//        if (event.packet instanceof BlockBreakingProgressS2CPacket packet) {
//            PlayerEntity player = (PlayerEntity) mc.world.getEntityById(packet.getEntityId());
//            if (player == mc.player || !positions.contains(packet.getPos())) return;
//            if (breaking.contains(packet.getPos())) return;
//
//            if (notifyBreak.get()) warning(player.getName().getString() + " is breaking your surround!");
//            breaking.add(packet.getPos());
//        }
//
//        if (event.packet instanceof BlockUpdateS2CPacket packet) {
//            if (packet.getState() != Blocks.AIR.getDefaultState()) return;
//            breaking.remove(packet.getPos());
//        }

        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getEntity(mc.world) != mc.player) return;
            if (packet.getStatus() != 3) return;

            if (notifyToggle.get()) info("Toggled off because you died.");
            toggle();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        BlockUtil.excludeSides(positions).forEach((pos, sides) -> {
            event.renderer.box(pos,
                getPosColors(pos)[0],
                getPosColors(pos)[1],
                activeShapeMode.get(),
                sides
            );
        });
    }

    private Color[] getPosColors(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);

        if (state.getHardness(mc.world, pos) < 0) return new Color[]{safeSides.get(), safeLines.get()};
        else if (state.getBlock().getBlastResistance() >= 600) return new Color[]{normalSides.get(), normalLines.get()};
        else return new Color[]{unsafeSides.get(), unsafeLines.get()};
    }




}

//Set<BlockPos> feets = DynamicUtil.feetPos(mc.player);
//
//        for (BlockPos pos: feets) {
//int excludeDir = 0;
//
//            for (Direction dir : Direction.values()) {
//    if (feets.contains(pos.offset(dir))) {
//excludeDir |= Dir.get(dir);
//                }
//                    }
//
//                    positions.put(pos, excludeDir);
//        }
//
//Set<BlockPos> toes = DynamicUtil.underPos(mc.player);
//
//        for (BlockPos pos: toes) {
//int excludeDir = 0;
//
//            for (Direction dir : Direction.values()) {
//    if (toes.contains(pos.offset(dir))) {
//excludeDir |= Dir.get(dir);
//                }
//                    }
//
//                    positions.put(pos, excludeDir);
//        }
//@EventHandler
//public void onRender3D(Render3DEvent event) {
//    for (Map.Entry<BlockPos, Integer> entry: positions.entrySet()) {
//        event.renderer.box(
//            entry.getKey(),
//            new Color(255, 0, 0, 50),
//            new Color(255, 0, 0, 255),
//            ShapeMode.Both,
//            entry.getValue()
//        );
//    }
//}


