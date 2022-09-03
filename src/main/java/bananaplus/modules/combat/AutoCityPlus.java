package bananaplus.modules.combat;

import bananaplus.BananaPlus;
import bananaplus.utils.BEntityUtils;
import bananaplus.utils.BPlayerUtils;
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
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTarget = settings.createGroup("Targeting");
    private final SettingGroup sgToggles = settings.createGroup("Module Toggles");
    private final SettingGroup sgRender = settings.createGroup("Render");


    public enum Mode {
        Normal,
        Instant
    }


    // General
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("How AutoCity should try and mine blocks.")
            .defaultValue(Mode.Normal)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("instamine-delay")
            .description("Delay between mining a block in ticks.")
            .defaultValue(1)
            .min(0)
            .sliderMax(50)
            .visible(() -> mode.get() == Mode.Instant)
            .build()
    );

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("mining-packets")
            .description("The amount of mining packets to be sent in a bundle.")
            .defaultValue(1)
            .range(1,5)
            .sliderRange(1,5)
            .visible(() -> mode.get() == Mode.Normal)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switch to a pickaxe when AutoCity is enabled.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
            .name("support")
            .description("Place a block below a cityable positions.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> supportRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("support-range")
            .description("The range for placing support block.")
            .defaultValue(4.5)
            .range(0,6)
            .sliderRange(0,6)
            .visible(support::get)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates you towards the city block.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Sends a message when it is trying to city someone.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> instaToggle = sgGeneral.add(new IntSetting.Builder()
            .name("toggle-delay")
            .description("Amount of ticks the city block has to be air to auto toggle off.")
            .defaultValue(40)
            .min(0)
            .sliderMax(100)
            .visible(() -> mode.get() == Mode.Instant)
            .build()
    );


    // Toggles
    private final Setting<Boolean> turnOnBBomber = sgToggles.add(new BoolSetting.Builder()
            .name("turn-on-auto-crystal")
            .description("Automatically toggles Banana Bomber on if a block target is found.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> turnOnButtonTrap = sgToggles.add(new BoolSetting.Builder()
            .name("turn-on-button-trap")
            .description("Automatically toggles Button Trap on if a block target is found.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> turnOffInstaMine = sgToggles.add(new BoolSetting.Builder()
            .name("turn-off-instamine")
            .description("Automatically toggles Instamine off if a block target is found.")
            .defaultValue(false)
            .build()
    );


    // Targeting
    private final Setting<Double> targetRange = sgTarget.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(5)
            .range(0,6)
            .sliderRange(0,6)
            .build()
    );

    private final Setting<Double> mineRange = sgTarget.add(new DoubleSetting.Builder()
            .name("mining-range")
            .description("The radius which you can mine at.")
            .defaultValue(4)
            .range(0,6)
            .sliderRange(0,6)
            .build()
    );

    private final Setting<Boolean> prioBurrowed = sgTarget.add(new BoolSetting.Builder()
            .name("mine-burrows")
            .description("Will mine a target's burrow before citying them.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noCitySurrounded = sgTarget.add(new BoolSetting.Builder()
            .name("not-surrounded")
            .description("Will not city a target if they aren't surrounded.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> avoidSelf = sgTarget.add(new BoolSetting.Builder()
            .name("avoid-self")
            .description("Will avoid targeting your own surround.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> lastResort = sgTarget.add(new BoolSetting.Builder()
            .name("last-resort")
            .description("Will try to target your own surround if there are no other options.")
            .defaultValue(true)
            .visible(avoidSelf::get)
            .build()
    );


    // Rendering
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
            .name("render-swing")
            .description("Renders your swing client-side.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render-break")
            .description("Renders the block being broken.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 0, 25))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
    );


    public AutoCityPlus() {
        super(BananaPlus.COMBAT, "auto-city+", "Automatically mine a target's surround.");
    }


    private PlayerEntity playerTarget;
    private BlockPos blockTarget;

    private boolean sentMessage;
    private boolean supportMessage;
    private boolean burrowMessage;

    private int delayLeft;
    private boolean mining;
    private int count;
    private Direction direction;


    @Override
    public void onActivate() {
        sentMessage = false;
        supportMessage = false;
        burrowMessage = false;
        count = 0;
        mining = false;
        delayLeft = 0;
        blockTarget = null;

        if (mode.get() == Mode.Instant) {
            if (TargetUtils.isBadTarget(playerTarget, targetRange.get())) {
                PlayerEntity search = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.ClosestAngle);
                if (search != playerTarget) sentMessage = false;
                playerTarget = search;
            }

            if (TargetUtils.isBadTarget(playerTarget, targetRange.get())) {
                playerTarget = null;
                blockTarget = null;
                toggle();
                return;
            }

            if (prioBurrowed.get() && BEntityUtils.isBurrowed(playerTarget, BEntityUtils.BlastResistantType.Mineable)) {
                blockTarget = playerTarget.getBlockPos();
                if (!burrowMessage && chatInfo.get()) {
                    warning("Mining %s's burrow.", playerTarget.getEntityName());
                    burrowMessage = true;
                }
            } else if (avoidSelf.get()) {
                blockTarget = BEntityUtils.getTargetBlock(playerTarget);
                if (blockTarget == null && lastResort.get()) blockTarget = BEntityUtils.getCityBlock(playerTarget);
            } else blockTarget = BEntityUtils.getCityBlock(playerTarget);
        }
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Instant && blockTarget != null) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockTarget, direction));
        }
        blockTarget = null;
        playerTarget = null;
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mode.get() == Mode.Normal) {
            if (TargetUtils.isBadTarget(playerTarget, targetRange.get())) {
                PlayerEntity search = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
                if (search != playerTarget) sentMessage = false;
                playerTarget = search;
            }

            if (TargetUtils.isBadTarget(playerTarget, targetRange.get())) {
                playerTarget = null;
                blockTarget = null;
                toggle();
                return;
            }

            if (prioBurrowed.get() && BEntityUtils.isBurrowed(playerTarget, BEntityUtils.BlastResistantType.Mineable)) {
                blockTarget = playerTarget.getBlockPos();
                if (!burrowMessage && chatInfo.get()) {
                    warning("Mining %s's burrow.", playerTarget.getEntityName());
                    burrowMessage = true;
                }
            } else if (noCitySurrounded.get() && !BEntityUtils.isSurrounded(playerTarget, BEntityUtils.BlastResistantType.Any)) {
                warning("%s is not surrounded... disabling", playerTarget.getEntityName());
                blockTarget = null;
                toggle();
                return;
            } else if (avoidSelf.get()) {
                blockTarget = BEntityUtils.getTargetBlock(playerTarget);
                if (blockTarget == null && lastResort.get()) blockTarget = BEntityUtils.getCityBlock(playerTarget);
            } else blockTarget = BEntityUtils.getCityBlock(playerTarget);
        }

        if (blockTarget == null) {
            error("No target block found... disabling.");
            toggle();
            playerTarget = null;
            return;
        } else if (!sentMessage && chatInfo.get() && blockTarget != playerTarget.getBlockPos()) {
            warning("Attempting to city %s.", playerTarget.getEntityName());
            sentMessage = true;
        }

        if (BPlayerUtils.distanceFromEye(blockTarget) > mineRange.get()) {
            error("Target block out of reach... disabling.");
            toggle();
            return;
        }

        if (turnOnBBomber.get() && blockTarget != null && !Modules.get().get(BananaBomber.class).isActive())
            Modules.get().get(BananaBomber.class).toggle();
        if (turnOnButtonTrap.get() && blockTarget != null && !Modules.get().get(AntiSurround.class).isActive())
            Modules.get().get(AntiSurround.class).toggle();
        if (turnOffInstaMine.get() && blockTarget != null && Modules.get().get(InstaMine.class).isActive())
            Modules.get().get(InstaMine.class).toggle();

        FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);

        if (!pickaxe.isHotbar()) {
            error("No pickaxe found... disabling.");
            toggle();
            return;
        }

        if (support.get() && !BEntityUtils.isBurrowed(playerTarget, BEntityUtils.BlastResistantType.Any)) {
            if (BPlayerUtils.distanceFromEye(blockTarget.down(1)) < supportRange.get()) {
                BlockUtils.place(blockTarget.down(1), InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            } else if (!supportMessage && blockTarget != playerTarget.getBlockPos()) {
                warning("Unable to support %s... mining anyway.", playerTarget.getEntityName());
                supportMessage = true;
            }
        }

        if (autoSwitch.get()) InvUtils.swap(pickaxe.slot(), false);

        if (mode.get() == Mode.Normal) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockTarget), Rotations.getPitch(blockTarget), () -> mine(blockTarget));
            else mine(blockTarget);
        }

        if (mode.get() == Mode.Instant) {
            if (playerTarget == null || !playerTarget.isAlive() || count >= instaToggle.get()) {
                toggle();
            }

            direction = BEntityUtils.rayTraceCheck(blockTarget, true);
            if (!mc.world.isAir(blockTarget)) {
                instamine(blockTarget);
            } else ++count;
        }
    }

    private void mine(BlockPos blockPos) {
        for (int packets = 0; packets < amount.get(); packets++) {
            if (!mining) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
                mining = true;
            }
        }
    }


    private void instamine(BlockPos blockPos) {
        --delayLeft;
        if (!mining) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockTarget), Rotations.getPitch(blockTarget));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
            mining = true;
        }
        if (delayLeft <= 0) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockTarget), Rotations.getPitch(blockTarget));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
            delayLeft = delay.get();
        }
    }


    @Override
    public String getInfoString() {
        return EntityUtils.getName(playerTarget);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || blockTarget == null) return;
        event.renderer.box(blockTarget, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
