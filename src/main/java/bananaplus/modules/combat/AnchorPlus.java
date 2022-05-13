package bananaplus.modules.combat;

import bananaplus.modules.BananaPlus;
import bananaplus.utils.Timer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Step;
import meteordevelopment.meteorclient.systems.modules.movement.speed.Speed;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class AnchorPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgToggle = settings.createGroup("Toggles");


    // General
    private final Setting<Integer> maxHeight = sgGeneral.add(new IntSetting.Builder()
            .name("max-height")
            .description("The maximum height Anchor will work at.")
            .defaultValue(10)
            .range(-64, 318)
            .sliderRange(-64, 318)
            .build()
    );

    private final Setting<Integer> minPitch = sgGeneral.add(new IntSetting.Builder()
            .name("min-pitch")
            .description("The minimum pitch at which anchor will work.")
            .defaultValue(75)
            .range(-90, 90)
            .sliderRange(-90, 90)
            .build()
    );

    private final Setting<Boolean> cancel = sgGeneral.add(new BoolSetting.Builder()
            .name("cancel-jump")
            .description("Prevents you from jumping when Anchor is active and Min Pitch is met.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pull = sgGeneral.add(new BoolSetting.Builder()
            .name("pull")
            .description("Whether Anchor should pull you into a hole.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> pullSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("pull-speed")
            .description("How fast Anchor should pull you into holes.")
            .defaultValue(0.3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> webs = sgGeneral.add(new BoolSetting.Builder()
            .name("Pull into webs")
            .description("Will also pull into webs.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> whileForward = sgGeneral.add(new BoolSetting.Builder()
            .name("while-forward")
            .description("Should anchor+ be active forward key is held.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> whileJumping = sgGeneral.add(new BoolSetting.Builder()
            .name("while-jumping")
            .description("Should anchor be active while jump key held.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> pullDelay = sgGeneral.add(new IntSetting.Builder()
            .name("Pull-Delay")
            .description("Amount of ticks anchor+ should wait before pulling you after you jump.")
            .defaultValue(14)
            .min(1)
            .sliderMax(60)
            .visible(() -> !whileJumping.get())
            .build()
    );

    private final Setting<Boolean> onGround = sgGeneral.add(new BoolSetting.Builder()
            .name("Pull-On-Ground")
            .description("If the pull delay should be reset when u land on the ground.")
            .defaultValue(true)
            .visible(() -> !whileJumping.get())
            .build()
    );


    //Toggles
    private final Setting<Boolean> turnOffStep = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-step")
            .description("Turns off Step on activation.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> turnOffStrafe = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-strafe")
            .description("Turns off Strafe on activation.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> turnOffSpeed = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-speed")
            .description("Turns off Speed on activation.")
            .defaultValue(false)
            .build()
    );


    public AnchorPlus() {
        super(BananaPlus.COMBAT, "anchor+", "Helps you get into holes by stopping your movement completely over a hole.");
    }


    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private boolean wasInHole;
    private boolean foundHole;
    private int holeX, holeZ;

    public boolean cancelJump;

    public boolean controlMovement;
    public double deltaX, deltaZ;

    private final Timer inAirTime = new Timer();
    boolean didJump = false;
    boolean pausing = false;


    @Override
    public void onActivate() {
        didJump = false;
        wasInHole = false;
        holeX = holeZ = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        cancelJump = foundHole && cancel.get() && mc.player.getPitch() >= minPitch.get();
        Modules modules = Modules.get();
        if (turnOffStep.get() && modules.get(Step.class).isActive()) modules.get(Step.class).toggle();
        if (turnOffStrafe.get() && modules.get(StrafePlus.class).isActive()) modules.get(StrafePlus.class).toggle();
        if (turnOffSpeed.get() && modules.get(Speed.class).isActive()) modules.get(Speed.class).toggle();
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if(!whileJumping.get()) {
            if(mc.options.jumpKey.isPressed()) {
                inAirTime.reset();
                didJump = true;
            }

            if (inAirTime.passedTicks(pullDelay.get()) && didJump || (onGround.get() && mc.player.isOnGround()))
            {
                didJump = false;
            }
        }

        if (!whileForward.get()) {
            pausing = mc.options.forwardKey.isPressed();
        } else pausing = false;

        if (didJump || pausing) return;

        controlMovement = false;

        int x = MathHelper.floor(mc.player.getX());
        int y = MathHelper.floor(mc.player.getY());
        int z = MathHelper.floor(mc.player.getZ());

        if (isHole(x, y, z)) {
            wasInHole = true;
            holeX = x;
            holeZ = z;
            return;
        }

        if (wasInHole && holeX == x && holeZ == z) return;
        else if (wasInHole) wasInHole = false;

        if (mc.player.getPitch() < minPitch.get()) return;

        foundHole = false;
        double holeX = 0;
        double holeZ = 0;

        for (int i = mc.world.getBottomY(); i < maxHeight.get(); i++) {
            y--;
            if (y <= mc.world.getBottomY() || !isAir(x, y, z)) break;

            if (isHole(x, y, z) && !(!webs.get() && isWeb(x,y,z))) {
                foundHole = true;
                holeX = x + 0.5;
                holeZ = z + 0.5;
                break;
            }
        }

        if (foundHole) {
            controlMovement = true;
            deltaX = Utils.clamp(holeX - mc.player.getX(), -0.05, 0.05);
            deltaZ = Utils.clamp(holeZ - mc.player.getZ(), -0.05, 0.05);

            ((IVec3d) mc.player.getVelocity()).set(deltaX, mc.player.getVelocity().y - (pull.get() ? pullSpeed.get() : 0), deltaZ);
        }
    }

    private boolean isHole(int x, int y, int z) {
        return isHoleBlock(x, y - 1, z) &&
                isHoleBlock(x + 1, y, z) &&
                isHoleBlock(x - 1, y, z) &&
                isHoleBlock(x, y, z + 1) &&
                isHoleBlock(x, y, z - 1);
    }

    private boolean isHoleBlock(int x, int y, int z) {
        blockPos.set(x, y, z);
        Block block = mc.world.getBlockState(blockPos).getBlock();
        return block == Blocks.BEDROCK
                || block == Blocks.OBSIDIAN
                || block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.ANVIL
                || block == Blocks.DAMAGED_ANVIL
                || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.NETHERITE_BLOCK
                || block == Blocks.ENDER_CHEST
                || block == Blocks.RESPAWN_ANCHOR
                || block == Blocks.ANCIENT_DEBRIS
                || block == Blocks.ENCHANTING_TABLE;
    }

    private boolean isAir(int x, int y, int z) {
        blockPos.set(x, y, z);
        return !((AbstractBlockAccessor)mc.world.getBlockState(blockPos).getBlock()).isCollidable();
    }

    private boolean isWeb(int x, int y, int z) {
        return isWebBlock(x, y, z);
    }

    private boolean isWebBlock(int x, int y, int z) {
        blockPos.set(x, y, z);
        Block block = mc.world.getBlockState(blockPos).getBlock();
        return block == Blocks.COBWEB;
    }
}
