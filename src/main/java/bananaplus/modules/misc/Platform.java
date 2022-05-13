package bananaplus.modules.misc;

import bananaplus.modules.BananaPlus;
import bananaplus.modules.combat.StrafePlus;
import bananaplus.utils.BPlusEntityUtils;
import bananaplus.utils.BPlusWorldUtils;
import bananaplus.utils.PositionUtils;
import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Step;
import meteordevelopment.meteorclient.systems.modules.movement.speed.Speed;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ChorusFruitItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Platform extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacing = settings.createGroup("Placing");
    private final SettingGroup sgForce = settings.createGroup("Force Keybinds");
    private final SettingGroup sgToggle = settings.createGroup("Toggle Modes");
    private final SettingGroup sgModules = settings.createGroup("Other Module Toggles");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("What blocks to use for Surround+.")
            .defaultValue(Blocks.OBSIDIAN)
            .build());

    private final Setting<List<Block>> fallbackBlocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("fallback-blocks")
            .description("What blocks to use for Surround+ if no target block is found.")
            .defaultValue(Blocks.CRYING_OBSIDIAN)
            .build());

    private final Setting<Integer> platformRange = sgGeneral.add(new IntSetting.Builder()
            .name("platform-range")
            .description("The range to platform around you.")
            .defaultValue(2)
            .range(1,5)
            .sliderRange(1,5)
            .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Tick delay between block placements.")
            .defaultValue(0)
            .build());

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("blocks-per-interval")
            .description("Blocks placed per delay interval.")
            .defaultValue(5)
            .min(1)
            .sliderMin(1)
            .sliderMax(20)
            .build());

    private final Setting<Boolean> doubleHeight = sgGeneral.add(new BoolSetting.Builder()
            .name("double-height")
            .description("Places below of the original surround blocks to prevent people from face-placing you.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Will only try to place if you are on the ground.")
            .defaultValue(false)
            .build());

    // Placing
    private final Setting<BPlusWorldUtils.SwitchMode> switchMode = sgPlacing.add(new EnumSetting.Builder<BPlusWorldUtils.SwitchMode>()
            .name("switch-mode")
            .description("How to switch to your target block.")
            .defaultValue(BPlusWorldUtils.SwitchMode.Both)
            .build());

    private final Setting<Boolean> switchBack = sgPlacing.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches back to your original slot after placing.")
            .defaultValue(true)
            .build());

    private final Setting<BPlusWorldUtils.PlaceMode> placeMode = sgPlacing.add(new EnumSetting.Builder<BPlusWorldUtils.PlaceMode>()
            .name("place-mode")
            .description("How to switch to your target block.")
            .defaultValue(BPlusWorldUtils.PlaceMode.Both)
            .build());

    private final Setting<Boolean> airPlace = sgPlacing.add(new BoolSetting.Builder()
            .name("air-place")
            .description("Whether to place blocks mid air or not.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> onlyAirPlace = sgPlacing.add(new BoolSetting.Builder()
            .name("only-air-place")
            .description("Forces you to only airplace to help with stricter rotations.")
            .defaultValue(false)
            .visible(airPlace::get)
            .build());

    private final Setting<BPlusWorldUtils.AirPlaceDirection> airPlaceDirection = sgPlacing.add(new EnumSetting.Builder<BPlusWorldUtils.AirPlaceDirection>()
            .name("air-place-direction")
            .description("Side to try to place at when you are trying to air place.")
            .defaultValue(BPlusWorldUtils.AirPlaceDirection.Down)
            .visible(airPlace::get)
            .build());

    private final Setting<Boolean> rotate = sgPlacing.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Whether to face towards the block you are placing or not.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> rotationPrio = sgPlacing.add(new IntSetting.Builder()
            .name("rotation-priority")
            .description("Rotation priority for Surround+.")
            .defaultValue(97)
            .sliderRange(0, 200)
            .visible(rotate::get)
            .build());

    private final Setting<Boolean> cancelMove = sgGeneral.add(new BoolSetting.Builder()
            .name("cancel-jump")
            .description("Prevents you from jumping.")
            .defaultValue(false)
            .build());

    // Force keybinds
    private final Setting<Keybind> doubleHeightKeybind = sgForce.add(new KeybindSetting.Builder()
            .name("double-height-keybind")
            .description("Turns on double height.")
            .defaultValue(Keybind.none())
            .build());

    // Toggles
    private final Setting<Boolean> toggleOnYChange = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-y-change")
            .description("Automatically disables when your y level (step, jumping, etc).")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> toggleOnComplete = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-complete")
            .description("Automatically disables when all blocks are placed.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> onPearl = sgToggle.add(new BoolSetting.Builder()
            .name("disable-on-pearl")
            .description("Automatically disables when you throw a pearl (work if u use middle/bind click extra).")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> onChorus = sgToggle.add(new BoolSetting.Builder()
            .name("disable-on-chorus")
            .description("Automatically disables after you eat a chorus.")
            .defaultValue(true)
            .build());

    // Modules
    private final Setting<Boolean> toggleStep = sgModules.add(new BoolSetting.Builder()
            .name("toggle-step")
            .description("Toggles off step when activating surround.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> toggleSpeed = sgModules.add(new BoolSetting.Builder()
            .name("toggle-speed")
            .description("Toggles off speed when activating surround.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> toggleStrafe = sgModules.add(new BoolSetting.Builder()
            .name("toggle-strafe+")
            .description("Toggles off strafe+ when activating surround.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> toggleBack = sgModules.add(new BoolSetting.Builder()
            .name("toggle-back")
            .description("Toggles the modules above back on if it was on previously when turning Surround+.")
            .defaultValue(false)
            .build());

    // Render
    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
            .name("render-swing")
            .description("Renders hand swing when trying to place a block.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where the block will be placed.")
            .defaultValue(true)
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(render::get)
            .build());

    private final Setting<SettingColor> placeColor = sgRender.add(new ColorSetting.Builder()
            .name("place-box-color")
            .description("The color of placing blocks.")
            .defaultValue(new SettingColor(255, 255, 255, 25))
            .visible(render::get)
            .build());

    private final Setting<SettingColor> placeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("place-line-color")
            .description("The color of placing line.")
            .defaultValue(new SettingColor(255, 255, 255, 150))
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

    public Platform() {
        super(BananaPlus.MISC, "platform", "Platforms around your feet to make building easier.");
    }

    // Fields
    private BlockPos playerPos;
    private int ticksPassed;
    private int blocksPlaced;

    public boolean cancelJump;

    // Modules
    Modules modules = Modules.get();
    Step step = modules.get(Step.class);
    Speed speed = modules.get(Speed.class);
    StrafePlus strafe = modules.get(StrafePlus.class);

    private boolean stepWasActive, speedWasActive, strafeWasActive;

    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();


    @Override
    public void onActivate() {
        ticksPassed = 0;
        blocksPlaced = 0;

        playerPos = BPlusEntityUtils.playerPos(mc.player);

        if (toggleStep.get() && step.isActive()) {
            stepWasActive = true;
            step.toggle();
        }
        if (toggleSpeed.get() && speed.isActive()) {
            speedWasActive = true;
            speed.toggle();
        }
        if (toggleStrafe.get() && strafe.isActive()) {
            strafeWasActive = true;
            strafe.toggle();
        }

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        if (toggleBack.get()) {
            if (stepWasActive && !step.isActive()) step.toggle();
            if (speedWasActive && !speed.isActive()) speed.toggle();
            if (strafeWasActive && !strafe.isActive()) strafe.toggle();
        }

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

        // Update player position
        playerPos = BPlusEntityUtils.playerPos(mc.player);

        if (toggleOnYChange.get()) {
            if (mc.player.prevY < mc.player.getY()) {
                toggle();
                return;
            }
        }

        if (toggleOnComplete.get()) {
            if (PositionUtils.allPlaced(placePos())) {
                toggle();
                return;
            }
        }

        if (onlyGround.get() && !mc.player.isOnGround()) return;

        if (!getTargetBlock().found()) return;

        cancelJump = cancelMove.get();

        if (ticksPassed <= 0) {
            for (BlockPos pos : centerPos()) {
                if (blocksPlaced >= blocksPerTick.get()) return;
                if (BPlusWorldUtils.place(pos, getTargetBlock(), rotate.get(), rotationPrio.get(), switchMode.get(), placeMode.get(), onlyAirPlace.get(), airPlaceDirection.get(), renderSwing.get(), true, switchBack.get())) {
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

        for (int x = -platformRange.get(); x <= platformRange.get(); x++) {
            for (int z = -platformRange.get(); z <= platformRange.get(); z++) {
                pos.add(new BlockPos(playerPos.getX() + x, playerPos.getY() - 1, playerPos.getZ() + z));
            }
        }

        return pos;
    }

    // This is the list around the center positions that doesn't need ignore entity
    private List<BlockPos> extraPos() {
        List<BlockPos> pos = new ArrayList<>();

        // Double Height
        if (doubleHeight.get() || doubleHeightKeybind.get().isPressed()) {
            for (BlockPos centerPos : centerPos()) {
                add(pos, centerPos.down());
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

    //Toggle
    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerInteractItemC2SPacket && (mc.player.getOffHandStack().getItem() instanceof EnderPearlItem || mc.player.getMainHandStack().getItem() instanceof EnderPearlItem) && onPearl.get()) {
            toggle();
        }
    }

    @EventHandler
    private void onFinishUsingItem(FinishUsingItemEvent event) {
        if (event.itemStack.getItem() instanceof ChorusFruitItem && onChorus.get()) {
            toggle();
        }
    }

    // Render
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            for (BlockPos pos : placePos()) {
                renderPos.set(pos);
                renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                renderBlocks.forEach(renderBlock -> renderBlock.render(event, placeColor.get(), placeLineColor.get(), shapeMode.get()));
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
}
