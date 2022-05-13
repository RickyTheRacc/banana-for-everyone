package bananaplus.modules.combat;

import bananaplus.modules.BananaPlus;
import bananaplus.utils.BPlusEntityUtils;
import bananaplus.utils.BPlusWorldUtils;
import bananaplus.utils.PositionUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AutoTrapPlus extends Module {
    public enum TopMode {
        Full,
        Top,
        Side,
        None
    }

    public enum BottomMode {
        Full,
        Single,
        Platform,
        None
    }


    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacing = settings.createGroup("Placing");
    private final SettingGroup sgRender = settings.createGroup("Render");


    // Targeting
    private final Setting<Double> targetRange = sgTargeting.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The range players can be targeted.")
            .defaultValue(4)
            .sliderRange(0,5)
            .build()
    );

    private final Setting<SortPriority> targetPriority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to select the player to target.")
            .defaultValue(SortPriority.ClosestAngle)
            .build()
    );


    // General
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("primary-blocks")
            .description("What blocks to use for Auto Trap+")
            .defaultValue(Blocks.OBSIDIAN)
            .filter(this::blockFilter)
            .build()
    );

    private final Setting<List<Block>> fallbackBlocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("fallback-blocks")
            .description("What blocks to use for Auto Trap+ if no target block is found.")
            .defaultValue(Blocks.RESPAWN_ANCHOR)
            .filter(this::blockFilter)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("place-delay")
            .description("Tick delay between block placements.")
            .defaultValue(0)
            .range(0,20)
            .sliderRange(0,20)
            .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("block-per-tick")
            .description("Blocks placed per delay interval.")
            .defaultValue(4)
            .range(1,5)
            .sliderRange(1,5)
            .build()
    );

    private final Setting<TopMode> topMode = sgGeneral.add(new EnumSetting.Builder<TopMode>()
            .name("top-mode")
            .description("The mode at which Auto Trap+ operates in.")
            .defaultValue(TopMode.Side)
            .build()
    );

    private final Setting<BottomMode> bottomMode = sgGeneral.add(new EnumSetting.Builder<BottomMode>()
            .name("bottom-mode")
            .description("The mode at which Auto Trap+ operates in.")
            .defaultValue(BottomMode.None)
            .build()
    );

    private final Setting<Boolean> dynamic = sgGeneral.add(new BoolSetting.Builder()
            .name("dynamic")
            .description("Will check for your hitbox to find placing positions.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder()
            .name("predict-movement")
            .description("Predicts target movement.")
            .defaultValue(false)
            .visible(dynamic::get)
            .build()
    );

    private final Setting<Boolean> antiVClip = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-vclip")
            .description("Prevents target from Vclipping out.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> antiVClipHeight = sgGeneral.add(new IntSetting.Builder()
            .name("top-height")
            .description("How high should it block enemy's anti-vclip.")
            .defaultValue(2)
            .range(1,6)
            .sliderRange(1,6)
            .visible(antiVClip::get)
            .build()
    );

    private final Setting<Boolean> toggleOnComplete = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-toggle")
            .description("Automatically disables when all blocks are placed.")
            .defaultValue(false)
            .build()
    );


    // Placing
    private final Setting<BPlusWorldUtils.SwitchMode> switchMode = sgPlacing.add(new EnumSetting.Builder<BPlusWorldUtils.SwitchMode>()
            .name("switch-mode")
            .description("How to switch to your target block.")
            .defaultValue(BPlusWorldUtils.SwitchMode.Both)
            .build()
    );

    private final Setting<Boolean> switchBack = sgPlacing.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches back to your original slot after placing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<BPlusWorldUtils.PlaceMode> placeMode = sgPlacing.add(new EnumSetting.Builder<BPlusWorldUtils.PlaceMode>()
            .name("place-mode")
            .description("How to switch to your target block.")
            .defaultValue(BPlusWorldUtils.PlaceMode.Both)
            .build()
    );

    private final Setting<Boolean> ignoreEntity = sgPlacing.add(new BoolSetting.Builder()
            .name("ignore-entities")
            .description("Whether to try to place over entities.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> airPlace = sgPlacing.add(new BoolSetting.Builder()
            .name("air-place")
            .description("Whether to place blocks in the air or not.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyAirPlace = sgPlacing.add(new BoolSetting.Builder()
            .name("only-air-place")
            .description("Forces you to only airplace. (Helps with stricter rotations)")
            .defaultValue(false)
            .visible(airPlace::get)
            .build()
    );

    private final Setting<BPlusWorldUtils.AirPlaceDirection> airPlaceDirection = sgPlacing.add(new EnumSetting.Builder<BPlusWorldUtils.AirPlaceDirection>()
            .name("fail-direction")
            .description("Side to try to place at when you are trying to air place.")
            .defaultValue(BPlusWorldUtils.AirPlaceDirection.Down)
            .visible(airPlace::get)
            .build()
    );

    private final Setting<Boolean> rotate = sgPlacing.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Whether to face towards the block you are placing or not.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> rotationPrio = sgPlacing.add(new IntSetting.Builder()
            .name("rotation-priority")
            .description("Rotation priority for Auto Trap+.")
            .defaultValue(98)
            .sliderRange(0, 200)
            .visible(rotate::get)
            .build()
    );


    // Render
    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
            .name("render-swing")
            .description("Renders hand swing when trying to place a block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renderPlace = sgRender.add(new BoolSetting.Builder()
            .name("render-place")
            .description("Will render where it is trying to place.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> placeSideColor = sgRender.add(new ColorSetting.Builder()
            .name("place-side-color")
            .description("The color of placing blocks.")
            .defaultValue(new SettingColor(255, 255, 255, 25))
            .visible(renderPlace::get)
            .build()
    );

    private final Setting<SettingColor> placeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("place-line-color")
            .description("The color of placing line.")
            .defaultValue(new SettingColor(255, 255, 255, 150))
            .visible(renderPlace::get)
            .build()
    );

    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder()
            .name("render-time")
            .description("Tick duration for rendering placing.")
            .defaultValue(8)
            .range(0,20)
            .sliderRange(0,20)
            .visible(renderPlace::get)
            .build()
    );

    private final Setting<Integer> fadeAmount = sgRender.add(new IntSetting.Builder()
            .name("fade-amount")
            .description("How long in ticks to fade out.")
            .defaultValue(8)
            .range(0,20)
            .sliderRange(0,20)
            .visible(renderPlace::get)
            .build()
    );

    private final Setting<Boolean> renderActive = sgRender.add(new BoolSetting.Builder()
            .name("render-active")
            .description("Renders blocks that are being surrounded.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Sides)
            .visible(renderActive::get)
            .build()
    );

    private final Setting<SettingColor> safeSideColor = sgRender.add(new ColorSetting.Builder()
            .name("safe-side-color")
            .description("The side color for safe blocks.")
            .defaultValue(new SettingColor(13, 255, 0, 15))
            .visible(() -> renderActive.get() && shapeMode.get() == ShapeMode.Sides)
            .build()
    );

    private final Setting<SettingColor> safeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("safe-line-color")
            .description("The line color for safe blocks.")
            .visible(() -> renderActive.get() && shapeMode.get() == ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> normalSideColor = sgRender.add(new ColorSetting.Builder()
            .name("normal-side-color")
            .description("The side color for normal blocks.")
            .defaultValue(new SettingColor(0, 255, 238, 15))
            .visible(() -> renderActive.get() && shapeMode.get() == ShapeMode.Sides)
            .build()
    );

    private final Setting<SettingColor> normalLineColor = sgRender.add(new ColorSetting.Builder()
            .name("normal-line-color")
            .description("The line color for normal blocks.")
            .defaultValue(new SettingColor(0, 255, 238, 125))
            .visible(() -> renderActive.get() && shapeMode.get() == ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> unsafeSideColor = sgRender.add(new ColorSetting.Builder()
            .name("unsafe-side-color")
            .description("The side color for unsafe blocks.")
            .defaultValue(new SettingColor(204, 0, 0, 15))
            .visible(() -> renderActive.get() && shapeMode.get() == ShapeMode.Sides)
            .build()
    );

    private final Setting<SettingColor> unsafeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("unsafe-line-color")
            .description("The line color for unsafe blocks.")
            .defaultValue(new SettingColor(204, 0, 0, 125))
            .visible(() -> renderActive.get() && shapeMode.get() == ShapeMode.Lines)
            .build()
    );


    public AutoTrapPlus() {
        super(BananaPlus.COMBAT, "auto-trap+", "Surround your target with blocks.");
    }


    private BlockPos playerPos;
    private int ticksPassed;
    private int blocksPlaced;

    private PlayerEntity target;

    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();


    @Override
    public void onActivate() {
        ticksPassed = 0;
        blocksPlaced = 0;

        target = null;

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        // Decrement placing timer
        if (ticksPassed >= 0) ticksPassed--;
        else {
            ticksPassed = delay.get();
            blocksPlaced = 0;
        }

        target = TargetUtils.getPlayerTarget(targetRange.get(), targetPriority.get());
        if (target == null) return;

        // Update player position
        playerPos = BPlusEntityUtils.playerPos(target);

        if (toggleOnComplete.get()) {
            if (PositionUtils.allPlaced(placePos())) {
                toggle();
                return;
            }
        }

        if (!getTargetBlock().found()) return;

        if (ticksPassed <= 0) {
            for (BlockPos pos : centerPos()) {
                if (blocksPlaced >= blocksPerTick.get()) return;
                if (BPlusWorldUtils.place(pos, getTargetBlock(), rotate.get(), rotationPrio.get(), switchMode.get(), placeMode.get(), onlyAirPlace.get(), airPlaceDirection.get(), renderSwing.get(), !ignoreEntity.get(), switchBack.get())) {
                    renderBlocks.add(renderBlockPool.get().set(pos));
                    blocksPlaced++;
                }
            }

            for (BlockPos pos : extraPos()) {
                if (blocksPlaced >= blocksPerTick.get()) return;
                if (BPlusWorldUtils.place(pos, getTargetBlock(), rotate.get(), rotationPrio.get(), switchMode.get(), placeMode.get(), onlyAirPlace.get(), airPlaceDirection.get(), renderSwing.get(), true, switchBack.get())) {
                    renderBlocks.add(renderBlockPool.get().set(pos));
                    blocksPlaced++;
                }
            }
        }

        // Ticking fade animation
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
    }

    // This is to return both centerPos and extraPos
    private List<BlockPos> placePos() {
        List<BlockPos> pos = new ArrayList<>();

        // centerPos
        for (BlockPos centerPos : centerPos()) add(pos, centerPos);
        // extraPos
        for (BlockPos extraPos : extraPos()) add(pos, extraPos);

        return pos;
    }

    // This is the blocks around the player that will try to ignore entity if the option is on
    private List<BlockPos> centerPos() {
        List<BlockPos> pos = new ArrayList<>();

        if (!dynamic.get()) {
            // TopMode
            if (topMode.get() == TopMode.Top || topMode.get() == TopMode.Full) {
                add(pos, playerPos.up(2));
            }

            if (topMode.get() == TopMode.Side || topMode.get() == TopMode.Full) {
                add(pos, playerPos.up().north());
                add(pos, playerPos.up().east());
                add(pos, playerPos.up().south());
                add(pos, playerPos.up().west());
            }

            // BottomMode
            if (bottomMode.get() == BottomMode.Single || bottomMode.get() == BottomMode.Platform) {
                add(pos, playerPos.down());
            }
            if (bottomMode.get() == BottomMode.Full) {
                add(pos, playerPos.north());
                add(pos, playerPos.east());
                add(pos, playerPos.south());
                add(pos, playerPos.west());
            }
            if (bottomMode.get() == BottomMode.Platform) {
                add(pos, playerPos.north().down());
                add(pos, playerPos.east().down());
                add(pos, playerPos.south().down());
                add(pos, playerPos.west().down());
            }
        } else {

            if (topMode.get() == TopMode.Full || topMode.get() == TopMode.Top) {
                // Top positions above
                for (BlockPos dynatmicTopPos : PositionUtils.dynamicTopPos(target, predictMovement.get())) {
                    if (PositionUtils.dynamicTopPos(target, predictMovement.get()).contains(dynatmicTopPos)) pos.remove(dynatmicTopPos);
                    add(pos, dynatmicTopPos);
                }
            }

            if (topMode.get() == TopMode.Full || topMode.get() == TopMode.Side) {
                // Top positions around
                for (BlockPos dynamicHeadPos : PositionUtils.dynamicHeadPos(target, predictMovement.get())) {
                    if (PositionUtils.dynamicHeadPos(target, predictMovement.get()).contains(dynamicHeadPos)) pos.remove(dynamicHeadPos);
                    add(pos, dynamicHeadPos);
                }
            }

            if (bottomMode.get() == BottomMode.Full) {
                // Bottom positions around
                for (BlockPos dynamicFeetPos : PositionUtils.dynamicFeetPos(target, predictMovement.get())) {
                    if (PositionUtils.dynamicFeetPos(target, predictMovement.get()).contains(dynamicFeetPos)) pos.remove(dynamicFeetPos);
                    add(pos, dynamicFeetPos);
                }
            }

            if (bottomMode.get() != BottomMode.None) {
                // Bottom positions below
                for (BlockPos dynamicBottomPos : PositionUtils.dynamicBottomPos(target, predictMovement.get())) {
                    if (PositionUtils.dynamicBottomPos(target, predictMovement.get()).contains(dynamicBottomPos)) pos.remove(dynamicBottomPos);
                    add(pos, dynamicBottomPos);
                }
            }

        }

        return pos;
    }

    // This is the list around the center positions that doesn't need ignore entity
    private List<BlockPos> extraPos() {
        List<BlockPos> pos = new ArrayList<>();

        if (antiVClip.get()) {
            for (int y = 1; y <= antiVClipHeight.get(); y++) {
                add(pos, playerPos.up(2 + y));
            }
        }

        return pos;
    }


    // adds block to list and structure block if needed to place
    private void add(List<BlockPos> list, BlockPos pos) {
        if (mc.world.getBlockState(pos).isAir()
                && allAir(pos.north(), pos.east(), pos.south(), pos.west(), pos.up(), pos.down())
                && !airPlace.get()
        ) list.add(pos.down());
        list.add(pos);
    }

    private boolean allAir(BlockPos... pos) {
        return Arrays.stream(pos).allMatch(blockPos -> mc.world.getBlockState(blockPos).getMaterial().isReplaceable());
    }

    private boolean anyAir(BlockPos... pos) {
        return Arrays.stream(pos).anyMatch(blockPos -> mc.world.getBlockState(blockPos).getMaterial().isReplaceable());
    }

    private FindItemResult getTargetBlock() {
        if (!InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))).found()) {
            return InvUtils.findInHotbar(itemStack -> fallbackBlocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        } else return InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
    }

    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN ||
                block == Blocks.CRYING_OBSIDIAN ||
                block == Blocks.ANCIENT_DEBRIS ||
                block == Blocks.NETHERITE_BLOCK ||
                block == Blocks.ENDER_CHEST ||
                block == Blocks.RESPAWN_ANCHOR ||
                block == Blocks.ANVIL ||
                block == Blocks.CHIPPED_ANVIL ||
                block == Blocks.DAMAGED_ANVIL ||
                block == Blocks.ENCHANTING_TABLE ||
                block == Blocks.COBWEB;
    }

    // Render
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (renderActive.get() && target != null) {
            for (BlockPos pos : placePos()) {
                renderPos.set(pos);
                Color color = getSideColor(renderPos);
                Color lineColor = getLineColor(renderPos);
                event.renderer.box(renderPos, color, lineColor, shapeMode.get(), 0);

                if (renderPlace.get()) {
                    renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                    renderBlocks.forEach(renderBlock -> renderBlock.render(event, placeSideColor.get(), placeLineColor.get(), shapeMode.get()));
                }
            }
        }
    }

    public class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = renderTime.get();

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode) {
            int preSideA = sides.a;
            int preLineA = lines.a;

            sides.a *= (double) ticks / fadeAmount.get() ;
            lines.a *= (double) ticks / fadeAmount.get();

            event.renderer.box(pos, sides, lines, shapeMode, 0);

            sides.a = preSideA;
            lines.a = preLineA;
        }
    }

    private BlockType getBlockType(BlockPos pos) {
        BlockState blockState = mc.world.getBlockState(pos);
        // Unbreakable eg. bedrock
        if (blockState.getBlock().getHardness() < 0) return BlockType.Safe;
            // Blast resistant eg. obsidian
        else if (blockState.getBlock().getBlastResistance() >= 600) return BlockType.Normal;
            // Anything else
        else return BlockType.Unsafe;
    }

    private Color getLineColor(BlockPos pos) {
        return switch (getBlockType(pos)) {
            case Safe -> safeLineColor.get();
            case Normal -> normalLineColor.get();
            case Unsafe -> unsafeLineColor.get();
        };
    }

    private Color getSideColor(BlockPos pos) {
        return switch (getBlockType(pos)) {
            case Safe -> safeSideColor.get();
            case Normal -> normalSideColor.get();
            case Unsafe -> unsafeSideColor.get();
        };
    }

    public enum BlockType {
        Safe,
        Normal,
        Unsafe
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
