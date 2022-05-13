package bananaplus.modules.combat;

import bananaplus.modules.BananaPlus;
import bananaplus.utils.BPlusEntityUtils;
import bananaplus.utils.BPlusPlayerUtils;
import bananaplus.utils.CommonEnums;
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
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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

    private final Setting<CommonEnums.ConTypeInclAlways> when = sgGeneral.add(new EnumSetting.Builder<CommonEnums.ConTypeInclAlways>()
            .name("when")
            .description("When to start button trapping.")
            .defaultValue(CommonEnums.ConTypeInclAlways.Always)
            .build()
    );

    private final Setting<Integer> delaySetting = sgGeneral.add(new IntSetting.Builder()
            .name("place-delay")
            .description("How many ticks between block placements.")
            .defaultValue(1)
            .range(0,20)
            .sliderRange(0,20)
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

    private PlayerEntity target;
    private final List<BlockPos> placePositions = new ArrayList<>();
    private int delay;

    public AntiSurround() {
        super(BananaPlus.COMBAT, "anti-surround", "Place items inside the enemy's surround to break it.");
    }

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

        FindItemResult button = InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof AbstractButtonBlock);
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

            if (delay >= delaySetting.get() && placePositions.size() > 0) {
                BlockPos blockPos = placePositions.get(placePositions.size() - 1);
                if (BPlusPlayerUtils.distanceFromEye(blockPos) > placeRange.get()) return;

                if (BlockUtils.place(blockPos, button, rotate.get(), 50, renderSwing.get(), checkEntity.get(), swapBack.get()))
                    placePositions.remove(blockPos);

                delay = 0;
            } else delay++;
        }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderTrap.get() || placePositions.isEmpty()) return;
        for (BlockPos pos : placePositions)
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }


    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos)
                && mc.world.getBlockState(blockPos).getMaterial().isReplaceable()
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
        switch (when.get()) {
            case BothTrapped -> {
                return BPlusEntityUtils.isBothTrapped(target, BPlusEntityUtils.BlastResistantType.NotAir);
            }
            case AnyTrapped -> {
                return BPlusEntityUtils.isAnyTrapped(target, BPlusEntityUtils.BlastResistantType.NotAir);
            }
            case TopTrapped -> {
                return BPlusEntityUtils.isTopTrapped(target, BPlusEntityUtils.BlastResistantType.NotAir);
            }
            case FaceTrapped -> {
                return BPlusEntityUtils.isFaceSurrounded(target, BPlusEntityUtils.BlastResistantType.NotAir);
            }
            case Always -> {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}