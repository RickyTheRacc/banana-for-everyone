package bananaplus.modules.combat;

import bananaplus.modules.BananaPlus;
import bananaplus.utils.BPlusEntityUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class SmartHoleFill extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");


    // This is so ass i know -ben
    public SmartHoleFill() {
        super(BananaPlus.COMBAT, "smort-hole-fill", "Prevents players from going into holes");
    }

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(4)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-radius")
            .description("Vertical radius in which to search for holes.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> fillRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("fill-range")
            .description("Range from target to hole for it to fill")
            .defaultValue(1.5)
            .min(0)
            .sliderMin(0.5)
            .sliderMax(3)
            .build());

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("target-range")
            .defaultValue(7)
            .min(0)
            .sliderMin(1)
            .sliderMax(10)
            .build());

    private final Setting<Double> rangePlace = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("place-range")
            .defaultValue(5)
            .min(0)
            .sliderMin(1)
            .sliderMax(6)
            .build());

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to select the player to target.")
            .defaultValue(SortPriority.LowestDistance)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate").description("Whether to rotate or not.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder()
            .name("only-in-hole").description("will only fill hole when u are in a hole")
            .defaultValue(false)
            .build());

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("What blocks to use for Auto Trap+.")
            .defaultValue(Blocks.OBSIDIAN)
            .filter(this::blockFilter)
            .build());

    /*
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
            .name("place-delay")
            .description("Place delay in ticks")
            .defaultValue(5)
            .min(1)
            .sliderMin(1)
            .sliderMax(20)
            .build());

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("blocks-per-tick")
            .description("Blocks placed per delay interval.")
            .defaultValue(5)
            .min(1)
            .sliderMin(1)
            .sliderMax(20)
            .build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where the obsidian will be placed.")
            .defaultValue(true)
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(render::get)
            .build());

    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder()
            .name("render-time")
            .description("Tick duration for rendering placing.")
            .defaultValue(8)
            .range(0, 40)
            .sliderRange(0, 40)
            .visible(render::get)
            .build());

    private final Setting<Integer> fadeAmount = sgRender.add(new IntSetting.Builder()
            .name("fade-amount")
            .description("How strong the fade should be.")
            .defaultValue(8)
            .range(0, 100)
            .sliderRange(0, 100)
            .visible(render::get)
            .build());

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
            .name("box-color")
            .description("The color of blocks.")
            .visible(render::get)
            .defaultValue(new SettingColor(255, 255, 255, 25))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of line.")
            .visible(render::get)
            .defaultValue(new SettingColor(255, 255, 255, 150))
            .build());

    */

    private final Pool<Hole> holePool = new Pool<>(Hole::new);
    private final List<Hole> holes = new ArrayList<>();

    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();

    //private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    //private final List<RenderBlock> renderBlocks = new ArrayList<>();

    //int blocksPlaced = 0;

    private final byte NULL = 0;

    private PlayerEntity target;


    @Override
    public void onActivate() {

        target = null;

        //for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        //renderBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        target = TargetUtils.getPlayerTarget(range.get(), priority.get());

        if ((onlyInHole.get() && (BPlusEntityUtils.isSurrounded(mc.player, BPlusEntityUtils.BlastResistantType.Any) || (BPlusEntityUtils.isInHole(mc.player, true, BPlusEntityUtils.BlastResistantType.Any)))) || (!onlyInHole.get())){
            for (Hole hole : holes) {

                if (target != null){


                    if (Math.sqrt(target.getPos().squaredDistanceTo(hole.blockPos.getX() + 0.5, hole.blockPos.getY() + 0.5, hole.blockPos.getZ() + 0.5)) <= fillRange.get()){

                        //info("dis: " + target.squaredDistanceTo(hole.blockPos.getX() + 0.5, hole.blockPos.getY() + 0.5, hole.blockPos.getZ() + 0.5));
                        if(!BlockUtils.place(hole.blockPos, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 50,true)) {

                        /*
                        blocksPlaced++;
                        if(blocksPlaced < blocksPerTick.get()) continue;
                        else {
                            blocksPlaced = 0;
                            return;
                        }*/

                            //renderBlocks.add(renderBlockPool.get().set(hole.blockPos));

                            return;
                        }
                    }

                }
            }
        }


        for (Hole hole : holes) holePool.free(hole);
        holes.clear();

        BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
            if (!validHole(blockPos)) return;

            int blocks = 0;
            Direction air = null;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP) continue;

                BlockState state = mc.world.getBlockState(blockPos.offset(direction));

                if (state.getBlock() != Blocks.AIR) blocks++;
                else if (direction == Direction.DOWN) return;
            }

            if (blocks == 5 && air == null) {
                 holes.add(holePool.get().set(blockPos, NULL));
            }
        });

        // Ticking fade animation
        //renderBlocks.forEach(RenderBlock::tick);
        //renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
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
                block == Blocks.ENCHANTING_TABLE;
    }

    private boolean validHole(BlockPos pos) {
        //check for if player is in the hole
        if ((mc.player.getBlockPos().equals(pos))) return false;

        //check for if the target is in the hole
        if (target != null){
            if ((target.getBlockPos().equals(pos))) return false;
        }

        if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) return false;

        //range check
        if (Math.sqrt(mc.player.getPos().squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) >= rangePlace.get()) return false;


        if (((AbstractBlockAccessor) mc.world.getBlockState(pos).getBlock()).isCollidable()) return false;

        //TODO: add a check for if hole is empty
        //maybe this below works?
        //if(mc.world.getOtherEntities(null, new Box(hole.blockPos)).stream().noneMatch(Entity::collides)) return false;

        return true;
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
    /*
    // Render
    @EventHandler
    private void onRender(Render3DEvent event) {

        renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
        renderBlocks.forEach(renderBlock -> renderBlock.render(event, color.get(), lineColor.get(), shapeMode.get()));
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
    }*/
}
