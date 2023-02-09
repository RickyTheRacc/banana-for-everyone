package bananaplus.modules.combat;

import bananaplus.BananaPlus;
import bananaplus.utils.BEntityUtils;
import bananaplus.utils.BWorldUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.orbit.EventPriority.LOWEST;

public class SmartHoleFill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRanges = settings.createGroup("Ranges");
    private final SettingGroup sgPlacing = settings.createGroup("Placing");
    private final SettingGroup sgRender = settings.createGroup("Render");


    // General
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("primary-blocks")
        .description("What blocks to use for filling holes.")
        .defaultValue(
            Blocks.COBWEB,
            Blocks.OBSIDIAN,
            Blocks.CRYING_OBSIDIAN,
            Blocks.NETHERITE_BLOCK
        )
        .filter(this::blockFilter)
        .build()
    );

    private final Setting<List<Block>> fallbackBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("fallback-blocks")
        .description("What blocks to use if no default blocks are found.")
        .defaultValue(Blocks.RESPAWN_ANCHOR)
        .filter(this::blockFilter)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Tick delay between block placements.")
        .defaultValue(1)
        .range(0,20)
        .sliderRange(0,20)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("Blocks placed per tick.")
        .defaultValue(4)
        .range(1,6)
        .sliderRange(1,6)
        .build()
    );

    private final Setting<Boolean> fillDoubles = sgGeneral.add(new BoolSetting.Builder()
        .name("fill-doubles")
        .description("Don't fill holes around surrounded targets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreInHole = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-in-hole")
        .description("Don't fill holes around surrounded targets.")
        .defaultValue(true)
        .build()
    );


    // Ranges
    private final Setting<Double> targetRange = sgRanges.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("target-range")
        .defaultValue(7)
        .min(0)
        .sliderMin(1)
        .sliderMax(10)
        .build()
    );

    private final Setting<Integer> searchRange = sgRanges.add(new IntSetting.Builder()
        .name("search-range")
        .description("The radius around you to look for holes.")
        .defaultValue(5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> fillRange = sgRanges.add(new DoubleSetting.Builder()
        .name("fill-range")
        .description("Range from target to hole for it to fill.")
        .defaultValue(2.5)
        .min(0)
        .sliderMin(0.5)
        .sliderMax(3)
        .build()
    );

    private final Setting<Double> placeRange = sgRanges.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("place-range")
        .defaultValue(5)
        .min(0)
        .sliderMin(1)
        .sliderMax(6)
        .build()
    );


    // Placing
    private final Setting<BWorldUtils.SwitchMode> switchMode = sgPlacing.add(new EnumSetting.Builder<BWorldUtils.SwitchMode>()
        .name("switch-mode")
        .description("How to switch to your target block.")
        .defaultValue(BWorldUtils.SwitchMode.Both)
        .build()
    );

    private final Setting<Boolean> switchBack = sgPlacing.add(new BoolSetting.Builder()
        .name("switch-back")
        .description("Switches back to your original slot after placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<BWorldUtils.PlaceMode> placeMode = sgPlacing.add(new EnumSetting.Builder<BWorldUtils.PlaceMode>()
        .name("place-mode")
        .description("How to switch to your target block.")
        .defaultValue(BWorldUtils.PlaceMode.Both)
        .build()
    );

    private final Setting<Boolean> ignoreEntity = sgPlacing.add(new BoolSetting.Builder()
        .name("ignore-entities")
        .description("Will try to place even if there is an entity in the way.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> airPlace = sgPlacing.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Whether to place blocks mid air or not.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyAirPlace = sgPlacing.add(new BoolSetting.Builder()
        .name("only-air-place")
        .description("Forces you to only airplace to help with stricter rotations.")
        .defaultValue(false)
        .visible(airPlace::get)
        .build()
    );

    private final Setting<BWorldUtils.AirPlaceDirection> airPlaceDirection = sgPlacing.add(new EnumSetting.Builder<BWorldUtils.AirPlaceDirection>()
        .name("place-direction")
        .description("Side to try to place at when you are trying to air place.")
        .defaultValue(BWorldUtils.AirPlaceDirection.Down)
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
        .description("Rotation priority for Self Trap+.")
        .defaultValue(99)
        .sliderRange(0, 200)
        .visible(rotate::get)
        .build()
    );


    // Render
    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
        .name("render-swing")
        .description("Render the player's hand swinging when placing blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderPlace = sgRender.add(new BoolSetting.Builder()
        .name("render-place")
        .description("Render the player's hand swinging when placing blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(renderPlace::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of blocks.")
        .visible(() -> renderPlace.get() && shapeMode.get() != ShapeMode.Lines)
        .defaultValue(new SettingColor(255, 255, 255, 25))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of lines.")
        .visible(() -> renderPlace.get() && shapeMode.get() != ShapeMode.Sides)
        .defaultValue(new SettingColor(255, 255, 255, 150))
        .build()
    );


    public SmartHoleFill() {
        super(BananaPlus.COMBAT, "smart-holefill", "Fill safe holes around your enemy.");
    }

    public final List<PlayerEntity> targets = new ArrayList<>();

    private final Pool<Hole> holePool = new Pool<>(Hole::new);
    private final List<Hole> holes = new ArrayList<>();

    private int delay, blocksPlaced;


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

    @Override
    public void onActivate() {
        delay = 0;
        blocksPlaced = 0;
    }

    @Override
    public void onDeactivate() {
        for (Hole hole : holes) holePool.free(hole);
        holes.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Decrement placing timer
        if (delay > 0) {
            delay--;
            return;
        } else {
            delay = placeDelay.get();
            blocksPlaced = 0;
        }

        for (Hole hole : holes) holePool.free(hole);
        holes.clear();

        setTargets();
        if (targets.isEmpty()) return;
        if (!getTargetBlock().found()) return;

        BlockIterator.register(searchRange.get(), searchRange.get(), (blockPos, blockState) -> {
            if (!validHole(blockPos)) return;

            int blocks = 0;
            Direction air = null;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP) continue;

                if (BEntityUtils.isBlastResistant(blockPos.offset(direction), BEntityUtils.BlastResistantType.Any)) blocks++;

                else if (direction == Direction.DOWN) continue;

                else if (validHole(blockPos.offset(direction)) && air == null) {
                    for (Direction dir : Direction.values()) {
                        if (dir == direction.getOpposite() || dir == Direction.UP) continue;
                        if (BEntityUtils.isBlastResistant(blockPos.offset(direction).offset(dir),  BEntityUtils.BlastResistantType.Any)) blocks++;
                        else continue;
                    }

                    air = direction;
                }
            }

            if (blocks == 5 && air == null) holes.add(holePool.get().set(blockPos, (byte) 0));
            else if (blocks == 8 && fillDoubles.get() && air != null) {
                holes.add(holePool.get().set(blockPos, Dir.get(air)));
            }
        });
    }

    @EventHandler (priority = LOWEST - 2)
    private void onPreTickLast(TickEvent.Pre event) {
        // Placing done in separate event because BlockIterators execute themselves at the end of a tick
        // instead of when they're called, this way it can still read the list they provide

        for (Hole hole : holes) {
            if (blocksPlaced >= blocksPerTick.get()) break;

            if (BWorldUtils.place(hole.blockPos, getTargetBlock(), rotate.get(), rotationPrio.get(), switchMode.get(), placeMode.get(), onlyAirPlace.get(), airPlaceDirection.get(), renderSwing.get(), !ignoreEntity.get(), switchBack.get()))
                blocksPlaced++;
        }
    }

    private FindItemResult getTargetBlock() {
        if (!InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))).found()) {
            return InvUtils.findInHotbar(itemStack -> fallbackBlocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        } else return InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
    }

    private void setTargets() {
        targets.clear();

        // Players
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isCreative() || player == mc.player || player.isDead() || !Friends.get().shouldAttack(player)
                || (BEntityUtils.isInHole(player, true, BEntityUtils.BlastResistantType.Any) && ignoreInHole.get())
            ) continue;

            if (player.distanceTo(mc.player) <= targetRange.get()) targets.add(player);
        }
    }

    private boolean validHole(BlockPos pos) {
        // Make sure the hole isn't occupied
        if (mc.player.getBlockPos().equals(pos)) return false;

        // Don't fill cobwebs
        if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) return false;

        // Ranges Check
        if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) > placeRange.get()) return false;

        // Doesn't have a collidable block
        if (((AbstractBlockAccessor) mc.world.getBlockState(pos).getBlock()).isCollidable()) return false;
        if (((AbstractBlockAccessor) mc.world.getBlockState(pos.up()).getBlock()).isCollidable()) return false;

        boolean validHole = false;

        for (PlayerEntity target : targets) {
            if (target.getY() > pos.getY()
                && !target.getBlockPos().equals(pos)
                &&  target.getPos().distanceTo(Vec3d.ofCenter(pos).add(0, 0.5, 0)) <= fillRange.get()
            ) validHole = true;
        }

        return validHole;
    }

    // Render
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (holes.isEmpty()) return;

        for (Hole hole : holes) {
            event.renderer.box(hole.blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), hole.exclude);
        }
    }

    private static class Hole {
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();
        public byte exclude;

        public Hole set(BlockPos blockPos, byte exclude) {
            this.blockPos.set(blockPos);
            this.exclude = exclude;

            return this;
        }
    }
}
