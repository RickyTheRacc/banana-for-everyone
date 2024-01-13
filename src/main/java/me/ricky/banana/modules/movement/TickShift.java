package me.ricky.banana.modules.movement;

import me.ricky.banana.BananaPlus;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class TickShift extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCharge = settings.createGroup("Charge");
    private final SettingGroup sgAnticheat = settings.createGroup("Anticheat");

    // General

    private final Setting<Integer> maxDuration = sgGeneral.add(new IntSetting.Builder()
        .name("max-duration")
        .description("How many ticks you are allowed to tick shift for.")
        .defaultValue(60)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("How far are you allowed to tick shift for.")
        .defaultValue(12)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Double> factor = sgGeneral.add(new DoubleSetting.Builder()
        .name("factor")
        .description("How fast to perform the tick shift.")
        .defaultValue(3.5)
        .min(1)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<Boolean> onJump = sgGeneral.add(new BoolSetting.Builder()
        .name("on-jump")
        .description("Whether the player needs to jump first or not.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-info")
        .description("Sends info about what the module is doing.")
        .defaultValue(false)
        .build()
    );

    // Charge

    private final Setting<Boolean> charge = sgCharge.add(new BoolSetting.Builder()
        .name("charge")
        .description("Whether or not to charge up your movements.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> chargeTicks = sgCharge.add(new IntSetting.Builder()
        .name("charge-ticks")
        .description("How many ticks to charge up your movement.")
        .defaultValue(30)
        .min(1)
        .sliderRange(1,50)
        .visible(charge::get)
        .build()
    );

    private final Setting<Boolean> lockMovement = sgCharge.add(new BoolSetting.Builder()
        .name("lock-movement")
        .description("Disables your movement when you are charging.")
        .defaultValue(false)
        .visible(charge::get)
        .build()
    );

    // Anti Cheat

    private final Setting<Boolean> inWater = sgAnticheat.add(new BoolSetting.Builder()
        .name("in-water")
        .description("Whether or not to allow you to tick shift in water.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> inLava = sgAnticheat.add(new BoolSetting.Builder()
        .name("in-lava")
        .description("Whether or not to allow you to tick shift in lava.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> whenSneaking = sgAnticheat.add(new BoolSetting.Builder()
        .name("when-sneaking")
        .description("Allow tick shift when sneaking.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> hungerCheck = sgAnticheat.add(new BoolSetting.Builder()
        .name("hunger-check")
        .description("Pauses when hunger reaches 3 or less drumsticks")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> step = sgAnticheat.add(new BoolSetting.Builder()
        .name("step")
        .description("Whether or not to allow you to step up on to blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> stepHeight = sgAnticheat.add(new DoubleSetting.Builder()
        .name("height")
        .description("How high are you allowed to step.")
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 10)
        .visible(step::get)
        .build()
    );

    public TickShift() {
        super(BananaPlus.FIXED, "tick-shift", "Allows you to charge up movement packets and move swiftly.");
    }

    private int chargeTicked;
    private int durationTicked;
    private boolean messaged;
    private boolean charged;
    private boolean moved;

    private final Vec3d startPos = new Vec3d(0, 0, 0);

    Timer timerClass = Modules.get().get(Timer.class);
    public Freecam freecam() {return Modules.get().get(Freecam.class);}

    @Override
    public void onActivate() {
        ((IVec3d) startPos).set(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        reset();
        messaged = false;
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    private void reset() {
        timerClass.setOverride(Timer.OFF);
        chargeTicked = 0;
        durationTicked = 0;

        charged = false;
        moved = false;

        if (step.get()) {
            mc.player.setStepHeight(0.5f);
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            if (chatInfo.get()) info("Rubberbanded, toggling.");
            toggle();
        }
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (!charged && lockMovement.get() && !freecam().isActive()) {
            if (!Input.isKeyPressed(GLFW.GLFW_KEY_W)
                && !Input.isKeyPressed(GLFW.GLFW_KEY_A)
                && !Input.isKeyPressed(GLFW.GLFW_KEY_S)
                && !Input.isKeyPressed(GLFW.GLFW_KEY_D)
                && !Input.isKeyPressed(GLFW.GLFW_KEY_SPACE)
            ) return;

            event.cancel();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Check pause settings
        if (mc.player.isFallFlying() || mc.player.isClimbing() || mc.player.getVehicle() != null) return;
        if (!whenSneaking.get() && mc.player.isSneaking()) return;
        if (!inWater.get() && mc.player.isTouchingWater()) return;
        if (!inLava.get() && mc.player.isInLava()) return;
        if (hungerCheck.get() && (mc.player.getHungerManager().getFoodLevel() <= 6)) return;

        // Make sure timer is off first
        timerClass.setOverride(Timer.OFF);

        if (charge.get()) {
            // Charge up meter
            chargeTicked++;
        }

        if (chargeTicked >= chargeTicks.get() || !charge.get()) charged = true;

        if (charged) {
            if (charge.get() && chatInfo.get() && !messaged) {
                warning("Charged!");
                messaged = true;
            }

            if (PlayerUtils.isMoving() && (!onJump.get() || (mc.options.jumpKey.isPressed() && onJump.get()))) moved = true;

            if (moved) {
                // Increment duration ticks
                durationTicked++;

                if (step.get()) {
                    mc.player.setStepHeight(stepHeight.get().floatValue());
                }
            }
        }

        if (startPos != null && (Math.sqrt(mc.player.squaredDistanceTo(startPos)) >= maxDistance.get() || (durationTicked * factor.get()) > maxDuration.get())) {
            toggle();
        }
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (!charged || !moved) return;

        // Now we override the timer
        timerClass.setOverride(factor.get());

        double moveForward = mc.player.input.movementForward;
        double moveStrafe = mc.player.input.movementSideways;
        double rotationYaw = mc.player.getYaw();

        if (moveForward == 0.0 && moveStrafe == 0.0) {
            ((IVec3d) event.movement).setXZ(0, 0);
        } else {
            if (moveForward != 0.0) {
                if (moveStrafe > 0.0) {
                    rotationYaw += (moveForward > 0.0 ? -45 : 45);
                } else if (moveStrafe < 0.0) {
                    rotationYaw += (moveForward > 0.0 ? 45 : -45);
                }
                moveStrafe = 0.0;
            }

            moveStrafe = moveStrafe == 0.0 ? moveStrafe : (moveStrafe > 0.0 ? 1.0 : -1.0);
            double rotationSin = Math.sin(Math.toRadians(rotationYaw + 90.0));
            ((IVec3d) event.movement).setXZ(
                moveForward * getMaxSpeed() * Math.cos(Math.toRadians(rotationYaw + 90.0) + moveStrafe * getMaxSpeed() * rotationSin),
                moveForward * getMaxSpeed() * rotationSin - moveStrafe * getMaxSpeed() * Math.cos(Math.toRadians(rotationYaw + 90.0))
            );
        }
    }

    private double getMaxSpeed() {
        double defaultSpeed = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            defaultSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            defaultSpeed /= 1.0 + 0.2 * (amplifier + 1);
        }
        return defaultSpeed;
    }

    @Override
    public String getInfoString() {
        if (charge.get()) {
            return (chargeTicked < chargeTicks.get() ? chargeTicked : "Charged") + "";
        } else return durationTicked + "";
    }
}