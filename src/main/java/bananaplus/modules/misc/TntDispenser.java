package bananaplus.modules.misc;

import bananaplus.modules.BananaPlus;
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
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class TntDispenser extends Module {
    public enum DispenserMode{
        Top,
        Around,
        Any
    }

    public enum LeverMode{
        Top,
        Around,
        Any
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTrap = settings.createGroup("Trap");
    private final SettingGroup sgDispensing = settings.createGroup("Dispensing");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgToggle = settings.createGroup("Toggle");
    private final SettingGroup sgTrapRender = settings.createGroup("Trap Render");
    private final SettingGroup sgDispensingRender = settings.createGroup("Dispensing Render");

    // General

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The range players can be targeted.")
            .defaultValue(4)
            .sliderRange(0,7)
            .range(0,7)
            .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The range blocks can be placed.")
            .defaultValue(4.5)
            .sliderRange(0,7)
            .range(0,7)
            .build()
    );

    // Trap

    private final Setting<Integer> delay = sgTrap.add(new IntSetting.Builder()
            .name("place-delay")
            .description("How many ticks between block placements.")
            .defaultValue(1)
            .build()
    );

    private final Setting<Boolean> faceTrap = sgTrap.add(new BoolSetting.Builder()
            .name("face-trap")
            .description("Whether to trap their face or not")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgTrap.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates towards blocks when placing.")
            .defaultValue(false)
            .build()
    );

    // Dispenser

    private final Setting<DispenserMode> dispenserPlace = sgDispensing.add(new EnumSetting.Builder<DispenserMode>()
            .name("dispenser-mode")
            .description("Where can it place dispensers around their head.")
            .defaultValue(DispenserMode.Any)
            .build()
    );

    private final Setting<Integer> tntStacks = sgDispensing.add(new IntSetting.Builder()
            .name("TNT-stacks")
            .description("How many stacks of TNT to put in the dispenser.")
            .defaultValue(1)
            .range(0,9)
            .sliderRange(0,9)
            .build()
    );

    private final Setting<LeverMode> leverPlace = sgDispensing.add(new EnumSetting.Builder<LeverMode>()
            .name("lever-mode")
            .description("Where can it place lever around the dispensers.")
            .defaultValue(LeverMode.Any)
            .build()
    );

    private final Setting<Integer> flickDelay = sgDispensing.add(new IntSetting.Builder()
            .name("flick-delay")
            .description("Tick delay between each lever flicks.")
            .defaultValue(0)
            .range(0,10)
            .sliderRange(0,10)
            .build()
    );

    // Toggle
    private final Setting<Boolean> disableTarget = sgToggle.add(new BoolSetting.Builder()
            .name("disable-on-no-target")
            .description("Automatically toggles off if no target is found")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableObby = sgToggle.add(new BoolSetting.Builder()
            .name("disable-on-no-obsidian")
            .description("Automatically toggles off if no obsidian is found")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableDispenser = sgToggle.add(new BoolSetting.Builder()
            .name("disable-on-no-dispenser")
            .description("Automatically toggles off if no dispenser is found")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableTNT = sgToggle.add(new BoolSetting.Builder()
            .name("disable-on-no-TNT")
            .description("Automatically toggles off if no TNT is found")
            .defaultValue(false)
            .build()
    );

    // Pause

    private final Setting<Boolean> onlyHole = sgPause.add(new BoolSetting.Builder()
            .name("only-in-hole")
            .description("Only continues the module if you are in a safe hole")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiSelf = sgPause.add(new BoolSetting.Builder()
            .name("anti-self")
            .description("Prevents performing the module on yourself")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> holePause = sgPause.add(new BoolSetting.Builder()
            .name("pause-not-in-hole")
            .description("Pauses if target is not in hole or surrounded.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> burrowPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-burrow")
            .description("Pauses if target is burrowed")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> pauseAtHealth = sgPause.add(new DoubleSetting.Builder()
            .name("pause-health")
            .description("Pauses when you go below a certain health.")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses when eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses when drinking.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Pauses mining.")
            .defaultValue(false)
            .build()
    );

    // Trap Render

    private final Setting<Boolean> trapRender = sgTrapRender.add(new BoolSetting.Builder()
            .name("trap-render")
            .description("Renders an overlay where obsidian will be placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgTrapRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(trapRender::get)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgTrapRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232, 10))
            .visible(trapRender::get)
            .build()
    );

    private final Setting<SettingColor> lineColor = sgTrapRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232))
            .visible(trapRender::get)
            .build()
    );

    private final Setting<SettingColor> nextSideColor = sgTrapRender.add(new ColorSetting.Builder()
            .name("next-side-color")
            .description("The side color of the next block to be placed.")
            .defaultValue(new SettingColor(227, 196, 245, 10))
            .visible(trapRender::get)
            .build()
    );

    private final Setting<SettingColor> nextLineColor = sgTrapRender.add(new ColorSetting.Builder()
            .name("next-line-color")
            .description("The line color of the next block to be placed.")
            .defaultValue(new SettingColor(227, 196, 245))
            .visible(trapRender::get)
            .build()
    );

    // Dispenser Render

    private final Setting<Boolean> dispenserRender = sgDispensingRender.add(new BoolSetting.Builder()
            .name("dispense-render")
            .description("Renders an overlay where dispensers & lever will be placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> dispenserShapeMode = sgDispensingRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("dispenser-shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(dispenserRender::get)
            .build()
    );

    private final Setting<SettingColor> dispenserSideColor = sgDispensingRender.add(new ColorSetting.Builder()
            .name("dispenser-side-color")
            .description("The side color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232, 10))
            .visible(dispenserRender::get)
            .build()
    );

    private final Setting<SettingColor> dispenserLineColor = sgDispensingRender.add(new ColorSetting.Builder()
            .name("dispenser-line-color")
            .description("The line color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232))
            .visible(dispenserRender::get)
            .build()
    );

    private final Setting<ShapeMode> leverShapeMode = sgDispensingRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("lever-shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(dispenserRender::get)
            .build()
    );

    private final Setting<SettingColor> leverSideColor = sgDispensingRender.add(new ColorSetting.Builder()
            .name("lever-side-color")
            .description("The side color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232, 10))
            .visible(dispenserRender::get)
            .build()
    );

    private final Setting<SettingColor> leverLineColor = sgDispensingRender.add(new ColorSetting.Builder()
            .name("lever-line-color")
            .description("The line color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232))
            .visible(dispenserRender::get)
            .build()
    );


    private final List<BlockPos> placePositions = new ArrayList<>();
    private PlayerEntity target;
    private boolean placed;
    private int timer;

    public TntDispenser() {
        super(BananaPlus.COMBAT, "tnt-dispenser", "Traps people in an obsidian and perform mass amount of TNT trolling on them.");
    }

    @Override
    public void onActivate() {
        target = null;
        placePositions.clear();
        timer = 0;
        placed = false;
    }

    @Override
    public void onDeactivate() {
        placePositions.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        FindItemResult dispenser = InvUtils.findInHotbar(Items.DISPENSER);
        FindItemResult lever = InvUtils.findInHotbar(Items.LEVER);
        FindItemResult tnt = InvUtils.find(Items.TNT);

        if (!obsidian.isHotbar() && !obsidian.isOffhand()) {
            placePositions.clear();
            placed = false;
            return;
        }

        if (TargetUtils.isBadTarget(target, range.get())) target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (TargetUtils.isBadTarget(target, range.get())) return;

        fillPlaceArray(target);

        if (timer >= delay.get() && placePositions.size() > 0) {
            BlockPos blockPos = placePositions.get(placePositions.size() - 1);

            if (BlockUtils.place(blockPos, obsidian, rotate.get(), 50, true)) {
                placePositions.remove(blockPos);
                placed = true;
            }

            timer = 0;
        } else {
            timer++;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!trapRender.get() || placePositions.isEmpty()) return;

        for (BlockPos pos : placePositions) {
            boolean isFirst = pos.equals(placePositions.get(placePositions.size() - 1));

            Color side = isFirst ? nextSideColor.get() : sideColor.get();
            Color line = isFirst ? nextLineColor.get() : lineColor.get();

            event.renderer.box(pos, side, line, shapeMode.get(), 0);
        }
    }

    private void fillPlaceArray(PlayerEntity target) {
        placePositions.clear();
        BlockPos targetPos = target.getBlockPos();

    }


    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos) && BlockUtils.canPlace(blockPos)) placePositions.add(blockPos);
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}