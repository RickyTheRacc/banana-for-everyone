package bplusdevelopment.modules.combat;

import bplusdevelopment.modules.AddModule;
import bplusdevelopment.modules.combat.bananabomber.BananaBomber;
import bplusdevelopment.utils.BPlusEntityUtils;
import bplusdevelopment.utils.BPlusPlayerUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.InstaMine;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoCityPlus extends Module {
    private final SettingGroup sgMode = settings.createGroup("Mode");
    private final SettingGroup sgTarget = settings.createGroup("Targeting");
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    public enum Mode {
        Normal,
        Instant
    }

    private final Setting<Mode> mode = sgMode.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Method of citying target's surround")
            .defaultValue(Mode.Normal)
            .build());

    // Targeting
    private final Setting<Double> targetRange = sgTarget.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(5)
            .min(0)
            .sliderMax(6)
            .build());

    private final Setting<Double> mineRange = sgTarget.add(new DoubleSetting.Builder()
            .name("mining-range")
            .description("The radius which you can mine at.")
            .defaultValue(5)
            .min(0)
            .sliderMax(7)
            .build());

    private final Setting<Boolean> prioBurrowed = sgTarget.add(new BoolSetting.Builder()
            .name("prioritise-burrow")
            .description("Will prioritise targeting the burrow block.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> noCitySurrounded = sgTarget.add(new BoolSetting.Builder()
            .name("not-surrounded")
            .description("Will not city if the target is not surrounded.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> avoidSelf = sgTarget.add(new BoolSetting.Builder()
            .name("avoid-self")
            .description("Will avoid targeting self surround.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> lastResort = sgTarget.add(new BoolSetting.Builder()
            .name("last-resort")
            .description("Will try to target your own surround as final option.")
            .defaultValue(true)
            .visible(avoidSelf::get)
            .build());

    // General
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("instamine-delay")
            .description("Delay between instamine in ticks.")
            .defaultValue(1)
            .min(0)
            .sliderMax(50)
            .visible(() -> mode.get() == Mode.Instant)
            .build());

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("mining-packets")
            .description("The amount of mining packets to be sent in a bundle.")
            .defaultValue(1)
            .range(1, 5)
            .sliderRange(1, 5)
            .visible(() -> mode.get() == Mode.Normal)
            .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Auto switches to a pickaxe when AutoCity is enabled.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
            .name("support")
            .description("If there is no block below a city block it will place one before mining.")
            .defaultValue(true)
            .build());

    private final Setting<Double> supportRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("support-range")
            .description("The range for placing support block.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .visible(support::get)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates you towards the city block.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing")
            .description("Renders your swing client-side.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("self-toggle")
            .description("Automatically toggles off after activation.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Sends a message when it is trying to city someone.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> instaToggle = sgGeneral.add(new IntSetting.Builder()
            .name("auto-toggle-delay")
            .description("Amount of ticks the city block has to be air to auto toggle off.")
            .defaultValue(40)
            .min(0)
            .sliderMax(100)
            .visible(() -> mode.get() == Mode.Instant && selfToggle.get())
            .build());

    private final Setting<Boolean> turnOnBBomber = sgGeneral.add(new BoolSetting.Builder()
            .name("Turn-on-Banana-Bomber")
            .description("Automatically toggles Banana Bomber on if a block target is found.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> turnOnButtonTrap = sgGeneral.add(new BoolSetting.Builder()
            .name("Turn-on-Button-Trap")
            .description("Automatically toggles Button Trap on if a block target is found.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> turnOffInstaMine = sgGeneral.add(new BoolSetting.Builder()
            .name("Turn-off-Instamine")
            .description("Automatically toggles Instamine off if a block target is found.")
            .defaultValue(false)
            .build());

    // Rendering
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where the obsidian will be placed.")
            .defaultValue(true)
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(230, 75, 100, 10))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(230, 75, 100, 255))
            .build());

    private PlayerEntity target;
    private BlockPos blockPosTarget;
    private boolean sentMessage;
    private boolean supportMessage;
    private boolean burrowMessage;

    // for insta
    private int delayLeft;
    private boolean mining;
    private int count;
    private Direction direction;

    public AutoCityPlus() {
        super(AddModule.BANANAPLUS, "auto-city+", "Automatically cities a target by mining the nearest obsidian next to them.");
    }

    @Override
    public void onActivate() {
        sentMessage = false;
        supportMessage = false;
        burrowMessage = false;
        count = 0;
        mining = false;
        delayLeft = 0;
        blockPosTarget = null;

        if (mode.get() == Mode.Instant) {
            if (TargetUtils.isBadTarget(target, targetRange.get())) {
                PlayerEntity search = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
                if (search != target) sentMessage = false;
                target = search;
            }

            if (TargetUtils.isBadTarget(target, targetRange.get())) {
                target = null;
                blockPosTarget = null;
                if (selfToggle.get()) toggle();
                return;
            }

            if (prioBurrowed.get() && BPlusEntityUtils.isBurrowed(target, BPlusEntityUtils.BlastResistantType.Mineable)) {
                blockPosTarget = target.getBlockPos();
                if (!burrowMessage && chatInfo.get()) {
                    warning("Mining %s's burrow.", target.getEntityName());
                    burrowMessage = true;
                }
            } else if (avoidSelf.get()) {
                blockPosTarget = BPlusEntityUtils.getTargetBlock(target);
                if (blockPosTarget == null && lastResort.get()) blockPosTarget = BPlusEntityUtils.getCityBlock(target);
            } else blockPosTarget = BPlusEntityUtils.getCityBlock(target);
        }
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Instant && blockPosTarget != null) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPosTarget, direction));
        }
        blockPosTarget = null;
        target = null;
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mode.get() == Mode.Normal) {
            if (TargetUtils.isBadTarget(target, targetRange.get())) {
                PlayerEntity search = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
                if (search != target) sentMessage = false;
                target = search;
            }

            if (TargetUtils.isBadTarget(target, targetRange.get())) {
                target = null;
                blockPosTarget = null;
                if (selfToggle.get()) toggle();
                return;
            }

            if (prioBurrowed.get() && BPlusEntityUtils.isBurrowed(target, BPlusEntityUtils.BlastResistantType.Mineable)) {
                blockPosTarget = target.getBlockPos();
                if (!burrowMessage && chatInfo.get()) {
                    warning("Mining %s's burrow.", target.getEntityName());
                    burrowMessage = true;
                }
            } else if (noCitySurrounded.get() && !BPlusEntityUtils.isSurrounded(target, BPlusEntityUtils.BlastResistantType.Any)) {
                warning("%s is not surrounded... disabling", target.getEntityName());
                blockPosTarget = null;
                toggle();
                return;
            } else if (avoidSelf.get()) {
                blockPosTarget = BPlusEntityUtils.getTargetBlock(target);
                if (blockPosTarget == null && lastResort.get()) blockPosTarget = BPlusEntityUtils.getCityBlock(target);
            } else blockPosTarget = BPlusEntityUtils.getCityBlock(target);
        }

        if (blockPosTarget == null) {
            if (selfToggle.get()) {
                error("No target block found... disabling.");
                toggle();
            }
            target = null;
            return;
        } else if (!sentMessage && chatInfo.get() && blockPosTarget != target.getBlockPos()) {
            warning("Attempting to city %s.", target.getEntityName());
            sentMessage = true;
        }

        if (BPlusPlayerUtils.distanceFromEye(blockPosTarget) > mineRange.get() && selfToggle.get()) {
            error("Target block out of reach... disabling.");
            toggle();
            return;
        }

        Modules modules = Modules.get();
        if (turnOnBBomber.get() && blockPosTarget != null && !modules.get(BananaBomber.class).isActive())
            modules.get(BananaBomber.class).toggle();
        if (turnOnButtonTrap.get() && blockPosTarget != null && !modules.get(ButtonTrap.class).isActive())
            modules.get(ButtonTrap.class).toggle();
        if (turnOffInstaMine.get() && blockPosTarget != null && modules.get(InstaMine.class).isActive())
            modules.get(InstaMine.class).toggle();

        FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);

        if (!pickaxe.isHotbar()) {
            if (selfToggle.get()) {
                error("No pickaxe found... disabling.");
                toggle();
            }
            return;
        }

        if (support.get() && !BPlusEntityUtils.isBurrowed(target, BPlusEntityUtils.BlastResistantType.Any)) {
            if (BPlusPlayerUtils.distanceFromEye(blockPosTarget.down(1)) < supportRange.get()) {
                BlockUtils.place(blockPosTarget.down(1), InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            } else if (!supportMessage && blockPosTarget != target.getBlockPos()) {
                warning("Unable to support %s... mining anyway.", target.getEntityName());
                supportMessage = true;
            }
        }

        if (autoSwitch.get()) InvUtils.swap(pickaxe.slot(), false);

        if (mode.get() == Mode.Normal) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPosTarget), Rotations.getPitch(blockPosTarget), () -> mine(blockPosTarget));
            else mine(blockPosTarget);
        }

        if (mode.get() == Mode.Instant) {
            if (selfToggle.get()) {
                direction = BPlusEntityUtils.rayTraceCheck(blockPosTarget, true);
                if (!mc.world.isAir(blockPosTarget)) {
                    instamine(blockPosTarget);
                } else ++count;

                if (target == null || !target.isAlive() || count >= instaToggle.get()) {
                    toggle();
                }
            } else {
                if (target == null) return;
                direction = BPlusEntityUtils.rayTraceCheck(blockPosTarget, true);
                if (!mc.world.isAir(blockPosTarget)) {
                    instamine(blockPosTarget);
                }
                if (target == null || !target.isAlive()) {
                    toggle();
                }
            }
        }


    }

    private void mine(BlockPos blockPos) {
        for (int packets = 0; packets < amount.get(); packets++) {
            if (!mining) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                if (!swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                else mc.player.swingHand(Hand.MAIN_HAND);
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
                mining = true;
            }
        }
    }


    private void instamine(BlockPos blockPos) {
        --delayLeft;
        if (!mining) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPosTarget), Rotations.getPitch(blockPosTarget));
            if (!swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            else mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
            mining = true;
        }
        if (delayLeft <= 0) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPosTarget), Rotations.getPitch(blockPosTarget));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
            delayLeft = delay.get();
        }
    }


    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || blockPosTarget == null) return;
        event.renderer.box(blockPosTarget, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
