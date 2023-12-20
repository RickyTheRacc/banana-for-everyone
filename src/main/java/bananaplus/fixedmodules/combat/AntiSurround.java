package bananaplus.fixedmodules.combat;

import bananaplus.BananaPlus;
import bananaplus.enums.BlockType;
import bananaplus.enums.SwingMode;
import bananaplus.fixedutils.CombatUtil;
import bananaplus.fixedutils.DynamicUtil;
import bananaplus.fixedutils.PlacingUtil;
import bananaplus.system.BananaConfig;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AntiSurround extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The radius players can be in to be targeted.")
        .defaultValue(5)
        .range(0,7)
        .sliderRange(0,7)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to choose a target.")
        .defaultValue(SortPriority.ClosestAngle)
        .build()
    );

    private final Setting<Boolean> checkEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("check-entities")
        .description("Check if entities like crystals are blocking the place.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Which blocks to try and place inside someone's surround.")
        .defaultValue(Blocks.OAK_BUTTON, Blocks.OAK_PRESSURE_PLATE, Blocks.TRIPWIRE, Blocks.REDSTONE_WIRE)
        .filter((block) -> !BlockType.Resistance.resists(block.getDefaultState()))
        .build()
    );

    // Render

    private final Setting<SwingMode> swingMode = sgRender.add(new EnumSetting.Builder<SwingMode>()
        .name("swing-mode")
        .description("How to swing your hand when attempting to place.")
        .defaultValue(SwingMode.Both)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the target block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the block.")
        .defaultValue(new SettingColor(204, 0, 0, 25))
        .visible(() -> render.get() && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the block.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .visible(() -> render.get() && shapeMode.get().lines())
        .build()
    );

    public AntiSurround() {
        super(BananaPlus.FIXED, "anti-surround", "Place items inside the enemy's surround to break it.");
    }

    private final List<BlockPos> surroundBlocks = new ArrayList<>();
    private FindItemResult result;
    private PlayerEntity target;
    private BlockPos placePos;

    @Override
    public void onActivate() {
        boolean onlyHotbar = BananaConfig.get().switchMode.get().onlyHotbar();
        result = InvUtils.find(stack -> blocks.get().contains(Block.getBlockFromItem(stack.getItem())), 0, onlyHotbar ? 8 : 35);
        if (!result.found()) {
            toggle();
            return;
        }

        target = findTarget();
        if (target == null) {
            toggle();
            return;
        }

        placePos = findPlacePos();
        if (placePos == null) toggle();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        boolean onlyHotbar = BananaConfig.get().switchMode.get().onlyHotbar();
        result = InvUtils.find(stack -> blocks.get().contains(Block.getBlockFromItem(stack.getItem())), 0, onlyHotbar ? 8 : 35);
        if (!result.found()) {
            toggle();
            return;
        }

        if (!isValidTarget(target)) {
            toggle();
            return;
        }

        placePos = findPlacePos();
        if (placePos == null) toggle();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof BlockUpdateS2CPacket packet) {
            if (!packet.getPos().equals(placePos)) return;
            if (!packet.getState().isReplaceable()) return;
            if (checkEntities.get() && !mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), placePos, ShapeContext.absent())) return;
            
            if (PlacingUtil.tryPlace(placePos, result, swingMode.get())) {
                RenderUtils.renderTickingBlock(
                    placePos, sideColor.get(), lineColor.get(), shapeMode.get(),
                    0, 4, true, false
                );
                toggle();
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (placePos == null) return;

        event.renderer.box(placePos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private BlockPos findPlacePos() {
        surroundBlocks.clear();
        surroundBlocks.addAll(DynamicUtil.feetPos(target));
        surroundBlocks.addAll(DynamicUtil.underPos(target));

        surroundBlocks.removeIf(BlockType.Hardness::resists);
        surroundBlocks.removeIf(pos -> PlacingUtil.bestDirection(pos, false) == null);

        if (surroundBlocks.isEmpty()) return null;

        surroundBlocks.sort(Comparator.comparing(PlayerUtils::squaredDistanceTo));
        double range = BananaConfig.get().blockRange.get();
        if (PlayerUtils.squaredDistanceTo(surroundBlocks.get(0)) > range * range) return null;
        return surroundBlocks.get(0);
    }

    private PlayerEntity findTarget() {
        return (PlayerEntity) TargetUtils.get(entity -> {
            if (!(entity instanceof PlayerEntity player)) return false;
            return isValidTarget(player);
        }, priority.get());
    }

    private boolean isValidTarget(PlayerEntity player) {
        double distance = PlayerUtils.squaredDistanceTo(player);
        if (distance > targetRange.get() * targetRange.get()) return false;
        if (player.isDead() || player == mc.player) return false;
        if (!CombatUtil.isInHole(player, BlockType.Resistance)) return false;

        if (player instanceof FakePlayerEntity) return true;

        if (EntityUtils.getGameMode(player) != GameMode.SURVIVAL) return false;
        return !Friends.get().isFriend(player);
    }
}
