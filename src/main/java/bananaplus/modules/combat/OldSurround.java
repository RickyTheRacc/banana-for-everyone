package bananaplus.modules.combat;

import bananaplus.BananaPlus;
import bananaplus.system.BananaConfig;
import bananaplus.utils.BEntityUtils;
import bananaplus.utils.BWorldUtils;
import bananaplus.utils.PositionUtils;
import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ChorusFruitItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class OldSurround extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacing = settings.createGroup("Placing");
    private final SettingGroup sgAntiCity = settings.createGroup("Anti City");
    private final SettingGroup sgForce = settings.createGroup("Force Keybinds");
    private final SettingGroup sgToggle = settings.createGroup("Toggle Modes");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("primary-blocks")
            .description("What blocks to use for Surround+.")
            .defaultValue(Blocks.OBSIDIAN)
            .filter(this::blockFilter)
            .build()
    );

    private final Setting<List<Block>> fallbackBlocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("fallback-blocks")
            .description("What blocks to use for Surround+ if no target block is found.")
            .defaultValue(Blocks.ENDER_CHEST)
            .filter(this::blockFilter)
            .build()
    );


    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("blocks-per-tick")
            .description("Blocks placed per tick.")
            .defaultValue(4)
            .range(1,5)
            .sliderRange(1,5)
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("layout")
            .description("Where Banana+ should place blocks.")
            .defaultValue(Mode.Normal)
            .build()
    );

    private final Setting<CenterMode> centerMode = sgGeneral.add(new EnumSetting.Builder<CenterMode>()
            .name("center")
            .description("How Surround+ should center you.")
            .defaultValue(CenterMode.Center)
            .build()
    );

    private final Setting<Boolean> dynamic = sgGeneral.add(new BoolSetting.Builder()
            .name("dynamic")
            .description("Will check for your hitbox to find placing positions.")
            .defaultValue(false)
            .visible(() -> centerMode.get() == CenterMode.None)
            .build()
    );

    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Will only try to place if you are on the ground.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> toggleModules = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-modules")
            .description("Turn off other modules when surround is activated.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> toggleBack = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-back-on")
            .description("Turn the other modules back on when surround is deactivated.")
            .defaultValue(false)
            .visible(toggleModules::get)
            .build()
    );

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("modules")
            .description("Which modules to disable on activation.")
            /*.defaultValue(new ArrayList<>() {{
                add(Modules.get().get(Step.class));
                add(Modules.get().get(StepPlus.class));
                add(Modules.get().get(Speed.class));
                add(Modules.get().get(StrafePlus.class));
            }})*/
            .visible(toggleModules::get)
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

    private final Setting<Boolean> rotate = sgPlacing.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Whether to face towards the block you are placing or not.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> rotationPrio = sgPlacing.add(new IntSetting.Builder()
            .name("rotation-priority")
            .description("Rotation priority for Surround+.")
            .defaultValue(100)
            .sliderRange(0, 200)
            .visible(rotate::get)
            .build()
    );

    // Anti City

    private final Setting<Boolean> notifyBreak = sgAntiCity.add(new BoolSetting.Builder()
            .name("notify-break")
            .description("Notifies you when someone is mining your surround.")
            .defaultValue(false)
            .build()
    );

    private final Setting<AntiCityMode> antiCityMode = sgAntiCity.add(new EnumSetting.Builder<AntiCityMode>()
            .name("anti-city-mode")
            .description("Behaviour of anti city.")
            .defaultValue(AntiCityMode.None)
            .build()
    );

    private final Setting<AntiCityShape> antiCityShape = sgAntiCity.add(new EnumSetting.Builder<AntiCityShape>()
            .name("anti-city-shape")
            .description("Shape mode to use for anti city.")
            .defaultValue(AntiCityShape.Russian)
            .visible(() -> antiCityMode.get() != AntiCityMode.None)
            .build()
    );

    // Force

    private final Setting<Keybind> russianKeybind = sgForce.add(new KeybindSetting.Builder()
            .name("russian-keybind")
            .description("Turns on Russian surround when held.")
            .defaultValue(Keybind.none())
            .build()
    );

    private final Setting<Keybind> russianPlusKeybind = sgForce.add(new KeybindSetting.Builder()
            .name("russian+-keybind")
            .description("Turns on Russian+ when held")
            .defaultValue(Keybind.none())
            .build()
    );

    private final Setting<Keybind> centerKeybind = sgForce.add(new KeybindSetting.Builder()
            .name("center-keybind")
            .description("Re-center you when held.")
            .defaultValue(Keybind.none())
            .build()
    );

    // Toggles

    private final Setting<Boolean> toggleOnYChange = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-y-change")
            .description("Automatically disables when your Y level changes.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> toggleOnComplete = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-complete")
            .description("Automatically disables when all blocks are placed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> toggleOnPearl = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-pearl")
            .description("Automatically disables when you throw a pearl (work if u use middle/bind click extra).")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> toggleOnChorus = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-chorus")
            .description("Automatically disables after you eat a chorus.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> toggleOnDeath = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-death")
            .description("Automatically disables after you die.")
            .defaultValue(false)
            .build()
    );

    // Render

    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
            .name("render-swing")
            .description("Renders hand swing when trying to place a block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Lines)
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
            .visible(() -> renderPlace.get() && shapeMode.get() != ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> placeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("place-line-color")
            .description("The color of placing line.")
            .defaultValue(new SettingColor(255, 255, 255, 150))
            .visible(() -> renderPlace.get() && shapeMode.get() != ShapeMode.Sides)
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

    private final Setting<SettingColor> safeSideColor = sgRender.add(new ColorSetting.Builder()
            .name("safe-side-color")
            .description("The side color for safe blocks.")
            .defaultValue(new SettingColor(13, 255, 0, 15))
            .visible(() -> renderActive.get() && shapeMode.get() != ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> safeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("safe-line-color")
            .description("The line color for safe blocks.")
            .visible(() -> renderActive.get() && shapeMode.get() != ShapeMode.Sides)
            .build()
    );

    private final Setting<SettingColor> normalSideColor = sgRender.add(new ColorSetting.Builder()
            .name("normal-side-color")
            .description("The side color for normal blocks.")
            .defaultValue(new SettingColor(0, 255, 238, 15))
            .visible(() -> renderActive.get() && shapeMode.get() != ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> normalLineColor = sgRender.add(new ColorSetting.Builder()
            .name("normal-line-color")
            .description("The line color for normal blocks.")
            .defaultValue(new SettingColor(0, 255, 238, 125))
            .visible(() -> renderActive.get() && shapeMode.get() != ShapeMode.Sides)
            .build()
    );

    private final Setting<SettingColor> unsafeSideColor = sgRender.add(new ColorSetting.Builder()
            .name("unsafe-side-color")
            .description("The side color for unsafe blocks.")
            .defaultValue(new SettingColor(204, 0, 0, 15))
            .visible(() -> renderActive.get() && shapeMode.get() != ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> unsafeLineColor = sgRender.add(new ColorSetting.Builder()
            .name("unsafe-line-color")
            .description("The line color for unsafe blocks.")
            .defaultValue(new SettingColor(204, 0, 0, 125))
            .visible(() -> renderActive.get() && shapeMode.get() != ShapeMode.Sides)
            .build()
    );

    public OldSurround() {
        super(BananaPlus.COMBAT, "surround+", "Surrounds you in blocks to prevent you from taking lots of damage.");
    }

    private BlockPos playerPos;
    private int ticksPassed;
    private int blocksPlaced;

    private boolean centered;

    private BlockPos prevBreakPos;
    PlayerEntity prevBreakingPlayer = null;

    private boolean shouldRussianNorth;
    private boolean shouldRussianEast;
    private boolean shouldRussianSouth;
    private boolean shouldRussianWest;

    private boolean shouldRussianPlusNorth;
    private boolean shouldRussianPlusEast;
    private boolean shouldRussianPlusSouth;
    private boolean shouldRussianPlusWest;

    public ArrayList<Module> toActivate;

    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    @Override
    public void onActivate() {
        ticksPassed = 0;
        blocksPlaced = 0;

        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);

        toActivate = new ArrayList<>();

        if (centerMode.get() != CenterMode.None) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }

        if (toggleModules.get() && !modules.get().isEmpty() && mc.world != null && mc.player != null) {
            for (Module module : modules.get()) {
                if (module.isActive()) {
                    module.toggle();
                    toActivate.add(module);
                }
            }
        }

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        if (toggleBack.get() && !toActivate.isEmpty() && mc.world != null && mc.player != null) {
            for (Module module : toActivate) {
                if (!module.isActive()) {
                    module.toggle();
                }
            }
        }

        shouldRussianNorth = false;
        shouldRussianEast = false;
        shouldRussianSouth = false;
        shouldRussianWest = false;

        shouldRussianPlusNorth = false;
        shouldRussianPlusEast = false;
        shouldRussianPlusSouth = false;
        shouldRussianPlusWest = false;

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        // Decrement placing timer
        if (ticksPassed >= 0) ticksPassed--;
        else {
            ticksPassed = BananaConfig.get().placeDelay.get();
            blocksPlaced = 0;
        }

        // Update player position
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None && !centered && mc.player.isOnGround()) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();

            centered = true;
        }

        // Need to recenter again if the player is in the air
        if (!mc.player.isOnGround()) centered = false;

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

        if (ticksPassed <= 0) {
            for (BlockPos pos : centerPos()) {
                if (blocksPlaced >= blocksPerTick.get()) return;
                if (BWorldUtils.place(pos, getTargetBlock(), rotate.get(), rotationPrio.get(), switchMode.get(), placeMode.get(), renderSwing.get(), !ignoreEntity.get(), switchBack.get())) {
                    renderBlocks.add(renderBlockPool.get().set(pos));
                    blocksPlaced++;
                }
            }

            for (BlockPos pos : extraPos()) {
                if (blocksPlaced >= blocksPerTick.get()) return;
                if (BWorldUtils.place(pos, getTargetBlock(), rotate.get(), rotationPrio.get(), switchMode.get(), placeMode.get(), renderSwing.get(), true, switchBack.get())) {
                    renderBlocks.add(renderBlockPool.get().set(pos));
                    blocksPlaced++;
                }
            }
        }

        // Ticking fade animation
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (centerKeybind.get().isPressed()) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }
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
            add(pos, playerPos.down());
            add(pos, playerPos.north());
            add(pos, playerPos.east());
            add(pos, playerPos.south());
            add(pos, playerPos.west());
        } else {
            // Bottom positions
            for (BlockPos dynamicBottomPos : PositionUtils.dynamicBottomPos(mc.player, false)) {
                if (PositionUtils.dynamicBottomPos(mc.player, false).contains(dynamicBottomPos)) pos.remove(dynamicBottomPos);
                add(pos, dynamicBottomPos);
            }

            // Surround positions
            for (BlockPos dynamicFeetPos : PositionUtils.dynamicFeetPos(mc.player, false)) {
                if (PositionUtils.dynamicFeetPos(mc.player, false).contains(dynamicFeetPos)) pos.remove(dynamicFeetPos);
                add(pos, dynamicFeetPos);
            }
        }

        return pos;
    }

    // This is the list around the center positions that doesn't need ignore entity
    private List<BlockPos> extraPos() {
        List<BlockPos> pos = new ArrayList<>();

        // North
        if (mode.get() != Mode.Normal || russianKeybind.get().isPressed() || russianPlusKeybind.get().isPressed() || shouldRussianNorth || shouldRussianPlusNorth) {
            if (mc.world.getBlockState(playerPos.north()).getBlock() != Blocks.BEDROCK) {
                if (!dynamic.get()) add(pos, playerPos.north(2));
                else {
                    for (BlockPos plusPos : PositionUtils.dynamicRussianNorth(mc.player, false)) add(pos, plusPos);
                }
            }
        }
        if (mode.get() == Mode.Autist || russianPlusKeybind.get().isPressed() || shouldRussianPlusNorth) {
            if (mc.world.getBlockState(playerPos.north()).getBlock() != Blocks.BEDROCK) {
                if (!dynamic.get()) {
                    add(pos, playerPos.north().west());
                    add(pos, playerPos.north().east());
                } else {
                    for (BlockPos plusPos : PositionUtils.dynamicRussianNorth(mc.player, true)) add(pos, plusPos);
                }
            }
        }

        // East
        if (mode.get() != Mode.Normal || russianKeybind.get().isPressed() || russianPlusKeybind.get().isPressed() || shouldRussianEast || shouldRussianPlusEast) {
            if (mc.world.getBlockState(playerPos.east()).getBlock() != Blocks.BEDROCK) {
                if (!dynamic.get()) add(pos, playerPos.east(2));
                else {
                    for (BlockPos plusPos : PositionUtils.dynamicRussianEast(mc.player, false)) add(pos, plusPos);
                }
            }
        }
        if (mode.get() == Mode.Autist || russianPlusKeybind.get().isPressed() || shouldRussianPlusEast) {
            if (mc.world.getBlockState(playerPos.east()).getBlock() != Blocks.BEDROCK) {
                if (!dynamic.get()) {
                    add(pos, playerPos.east().north());
                    add(pos, playerPos.east().south());
                } else {
                    for (BlockPos plusPos : PositionUtils.dynamicRussianEast(mc.player, true)) add(pos, plusPos);
                }
            }
        }

        // South
        if (mode.get() != Mode.Normal || russianKeybind.get().isPressed() || russianPlusKeybind.get().isPressed() || shouldRussianSouth || shouldRussianPlusSouth) {
            if (mc.world.getBlockState(playerPos.south()).getBlock() != Blocks.BEDROCK) {
                if (!dynamic.get()) add(pos, playerPos.south(2));
                else {
                    for (BlockPos plusPos : PositionUtils.dynamicRussianSouth(mc.player, false)) add(pos, plusPos);
                }
            }
        }
        if (mode.get() == Mode.Autist || russianPlusKeybind.get().isPressed() || shouldRussianPlusSouth) {
            if (mc.world.getBlockState(playerPos.south()).getBlock() != Blocks.BEDROCK) {
                if (!dynamic.get()) {
                    add(pos, playerPos.south().east());
                    add(pos, playerPos.south().west());
                } else {
                    for (BlockPos plusPos : PositionUtils.dynamicRussianSouth(mc.player, true)) add(pos, plusPos);
                }
            }
        }

        // West
        if (mode.get() != Mode.Normal || russianKeybind.get().isPressed() || russianPlusKeybind.get().isPressed() || shouldRussianWest || shouldRussianPlusWest) {
            if (mc.world.getBlockState(playerPos.west()).getBlock() != Blocks.BEDROCK) {
                if (!dynamic.get()) add(pos, playerPos.west(2));
                else {
                    for (BlockPos plusPos : PositionUtils.dynamicRussianWest(mc.player, false)) add(pos, plusPos);
                }
            }
        }
        if (mode.get() == Mode.Autist || russianPlusKeybind.get().isPressed() || shouldRussianPlusWest) {
            if (mc.world.getBlockState(playerPos.west()).getBlock() != Blocks.BEDROCK) {
                if (!dynamic.get()) {
                    add(pos, playerPos.west().south());
                    add(pos, playerPos.west().north());
                } else {
                    for (BlockPos plusPos : PositionUtils.dynamicRussianWest(mc.player, true)) add(pos, plusPos);
                }
            }
        }

        return pos;
    }


    // adds block to list and structure block if needed to place
    private void add(List<BlockPos> list, BlockPos pos) {
        if (mc.world.getBlockState(pos).isAir()
                && allAir(pos.north(), pos.east(), pos.south(), pos.west(), pos.up(), pos.down())
                && !BananaConfig.get().airPlace.get()
        ) list.add(pos.down());
        list.add(pos);
    }

    private boolean allAir(BlockPos... pos) {
        return Arrays.stream(pos).allMatch(blockPos -> mc.world.getBlockState(blockPos).isReplaceable());
    }

    private boolean anyAir(BlockPos... pos) {
        return Arrays.stream(pos).anyMatch(blockPos -> mc.world.getBlockState(blockPos).isReplaceable());
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
                block == Blocks.ENCHANTING_TABLE;
    }

    @EventHandler
    public void onBreakPacket(PacketEvent.Receive event) {
        if(!(event.packet instanceof BlockBreakingProgressS2CPacket bbpp)) return;
        BlockPos bbp = bbpp.getPos();

        PlayerEntity breakingPlayer = (PlayerEntity) mc.world.getEntityById(bbpp.getEntityId());
        BlockPos playerBlockPos = mc.player.getBlockPos();

        if (bbpp.getProgress() > 0) return;
        if (bbp.equals(prevBreakPos)) return;
        // if (breakingPlayer == prevBreakingPlayer) return;
        // if u use this statement above, if the person that tries to mine your surround is the same again, it won't try to protect and alert you
        if (breakingPlayer.equals(mc.player)) return;

        if (bbp.equals(centerPos())) {
            if (antiCityMode.get() == AntiCityMode.All) {
                if (antiCityShape.get() == AntiCityShape.Russian) {
                    shouldRussianNorth = true;
                    shouldRussianEast = true;
                    shouldRussianSouth = true;
                    shouldRussianWest = true;
                } else {
                    shouldRussianPlusNorth = true;
                    shouldRussianPlusEast = true;
                    shouldRussianPlusSouth = true;
                    shouldRussianPlusWest = true;
                }
            }
        }

        // Todo : fix this for dynamic mode

        if (bbp.equals(playerBlockPos.north())) {
            if (antiCityMode.get() == AntiCityMode.Smart) {
                if (antiCityShape.get() == AntiCityShape.Russian) shouldRussianNorth = true;
                else shouldRussianPlusNorth = true;
            }
            if (notifyBreak.get()) notifySurroundBreak(Direction.NORTH, breakingPlayer);
        }

        if (bbp.equals(playerBlockPos.east())) {
            if (antiCityMode.get() == AntiCityMode.Smart) {
                if (antiCityShape.get() == AntiCityShape.Russian) shouldRussianEast = true;
                else shouldRussianPlusEast = true;
            }
            if (notifyBreak.get()) notifySurroundBreak(Direction.EAST, breakingPlayer);
        }

        if (bbp.equals(playerBlockPos.south())) {
            if (antiCityMode.get() == AntiCityMode.Smart) {
                if (antiCityShape.get() == AntiCityShape.Russian) shouldRussianSouth = true;
                else shouldRussianPlusSouth = true;
            }
            if (notifyBreak.get()) notifySurroundBreak(Direction.SOUTH, breakingPlayer);
        }

        if (bbp.equals(playerBlockPos.west())) {
            if (antiCityMode.get() == AntiCityMode.Smart) {
                if (antiCityShape.get() == AntiCityShape.Russian) shouldRussianWest = true;
                else shouldRussianPlusWest = true;
            }
            if (notifyBreak.get()) notifySurroundBreak(Direction.WEST, breakingPlayer);
        }

        prevBreakingPlayer = breakingPlayer;
        prevBreakPos = bbp;
    }

    private void notifySurroundBreak(Direction direction, PlayerEntity player) {
        warning("Your " + direction.toString() + " surround block is being broken by " + player.getName().getString());
    }

    //Toggle
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event)  {
        if (event.packet instanceof DeathMessageS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.getEntityId());
            if (entity == mc.player && toggleOnDeath.get()) {
                toggle();
            }
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerInteractItemC2SPacket && (mc.player.getOffHandStack().getItem() instanceof EnderPearlItem || mc.player.getMainHandStack().getItem() instanceof EnderPearlItem) && toggleOnPearl.get()) {
            toggle();
        }
    }

    @EventHandler
    private void onFinishUsingItem(FinishUsingItemEvent event) {
        if (event.itemStack.getItem() instanceof ChorusFruitItem && toggleOnChorus.get()) {
            toggle();
        }
    }

    // Render
    @EventHandler
    private void onRender(Render3DEvent event) {
        for (BlockPos pos : placePos()) {
            renderPos.set(pos);
            Color color = getBlockColor(renderPos);
            Color lineColor = getLineColor(renderPos);

            if (renderActive.get()) event.renderer.box(renderPos, color, lineColor, shapeMode.get(), 0);

            if (renderPlace.get()) {
                renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                renderBlocks.forEach(renderBlock -> renderBlock.render(event, placeSideColor.get(), placeLineColor.get(), shapeMode.get()));
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

    private Color getBlockColor(BlockPos pos) {
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

    public enum Mode {
        Normal,
        Russian,
        Autist
    }

    public enum CenterMode {
        Center,
        Snap,
        None
    }

    public enum AntiCityMode {
        Smart,
        All,
        None
    }

    public enum AntiCityShape {
        Russian,
        Autist,
    }

    public enum RenderMode {
        None,
        Normal,
        Place,
        Both
    }
}