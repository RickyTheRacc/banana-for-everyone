package bananaplus.modules.combat;

import bananaplus.BananaPlus;
import bananaplus.enums.BlockType;
import bananaplus.enums.TrapType;
import bananaplus.fixedutils.CombatUtil;
import bananaplus.system.BananaConfig;
import bananaplus.utils.BPlayerUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The radius buttons can be placed.")
            .defaultValue(4)
            .range(0,6)
            .sliderRange(0,6)
            .build()
    );

    private final Setting<TrapMode> when = sgGeneral.add(new EnumSetting.Builder<TrapMode>()
            .name("when")
            .description("When to start button trapping.")
            .defaultValue(TrapMode.Always)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Sends rotation packets to the server when placing.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> checkEntity = sgGeneral.add(new BoolSetting.Builder()
            .name("Check Entity")
            .description("Check if placing intersects with entities.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("Swap Back")
            .description("Swaps back to your previous slot after placing.")
            .defaultValue(true)
            .build()
    );

    // Render

    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
            .name("render-swing")
            .description("Renders your swing client-side.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renderTrap = sgRender.add(new BoolSetting.Builder()
            .name("render-trap")
            .description("Renders a block overlay where the button will be placed.")
            .defaultValue(true)
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
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
    );

    public AntiSurround() {
        super(BananaPlus.COMBAT, "anti-surround", "Place items inside the enemy's surround to break it.");
    }

    private PlayerEntity target;
    private final List<BlockPos> placePositions = new ArrayList<>();
    private int delay;

    @Override
    public void onActivate() {
        target = null;
        if (!placePositions.isEmpty()) placePositions.clear();
        delay = 0;
    }

    @EventHandler(priority = EventPriority.MEDIUM + 60)
    private void onTick(TickEvent.Pre event) {

        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);

        if (target == null || Objects.requireNonNull(mc.player).distanceTo(target) > targetRange.get()) {
            error("No target found, disabling...");
            toggle();
            return;
        } else {
            if (!isTrapped(target)) return;
        }

        FindItemResult button = InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof ButtonBlock);
        if (!button.found()) button = InvUtils.findInHotbar(Items.STRING);
        if (!button.found()) button = InvUtils.findInHotbar(Items.REDSTONE);
        // Check for enough resources
        if (!button.found()) {
            error("No button/string/redstone found in hotbar");
            toggle();
            return;
        }

            placePositions.clear();

            findPlacePos(target);

            if (delay >= BananaConfig.get().placeDelay.get() && placePositions.size() > 0) {
                BlockPos blockPos = placePositions.get(placePositions.size() - 1);
                if (BPlayerUtils.distanceFromEye(blockPos) > placeRange.get()) return;

                if (BlockUtils.place(blockPos, button, rotate.get(), 50, renderSwing.get(), checkEntity.get(), swapBack.get()))
                    placePositions.remove(blockPos);

                delay = 0;
            } else delay++;
        }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderTrap.get() || placePositions.isEmpty()) return;
        placePositions.forEach((pos) -> event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0));
    }


    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos)
                && mc.world.getBlockState(blockPos).isReplaceable()
                && mc.world.canPlace(Blocks.STONE_BUTTON.getDefaultState(), blockPos, ShapeContext.absent())
                && (mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ())).isFullCube(mc.world, new BlockPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ()))
                || mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ())).isFullCube(mc.world, new BlockPos(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ()))
                || mc.world.getBlockState(new BlockPos(blockPos.getX() + 1, blockPos.getY(), blockPos.getZ())).isFullCube(mc.world, new BlockPos(blockPos.getX() + 1, blockPos.getY(), blockPos.getZ()))
                || mc.world.getBlockState(new BlockPos(blockPos.getX() - 1, blockPos.getY(), blockPos.getZ())).isFullCube(mc.world, new BlockPos(blockPos.getX() - 1, blockPos.getY(), blockPos.getZ()))
                || mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() + 1)).isFullCube(mc.world, new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() + 1))
                || mc.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() - 1)).isFullCube(mc.world, new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() - 1))
        )
        ) {
            placePositions.add(blockPos);
        }
    }
    private void findPlacePos(PlayerEntity target) {
        placePositions.clear();
        BlockPos targetPos = target.getBlockPos();
        add(targetPos.add(1, 0, 0));
        add(targetPos.add(0, 0, 1));
        add(targetPos.add(-1, 0, 0));
        add(targetPos.add(0, 0, -1));
    }

    private boolean isTrapped(PlayerEntity target) {
        return switch (when.get()) {
            case BothTrapped -> CombatUtil.isTrapped(target, BlockType.NotEmpty, TrapType.Both);
            case AnyTrapped -> CombatUtil.isTrapped(target, BlockType.NotEmpty, TrapType.Any);
            case TopTrapped -> CombatUtil.isTrapped(target, BlockType.NotEmpty, TrapType.Top);
            case FaceTrapped -> CombatUtil.isTrapped(target, BlockType.NotEmpty, TrapType.Face);
            case Always -> true;
        };
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    public enum TrapMode {
        BothTrapped,
        AnyTrapped,
        TopTrapped,
        FaceTrapped,
        Always
    }
}