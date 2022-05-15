package bananaplus.modules.combat;

import bananaplus.modules.BananaPlus;
import bananaplus.utils.*;
import com.google.common.util.concurrent.AtomicDouble;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BedBlock;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MonkeSleeper extends Module {
    public enum YawStepMode {
        Break,
        All,
    }

    public enum DamageIgnore {
        Always,
        Never
    }

    public enum Decrementor {
        Place,
        Break,
        Both
    }

    public enum SelfPopIgnore {
        Place,
        Break,
        Both
    }

    public enum PopPause {
        Place,
        Break,
        Both
    }

    public enum RenderMode {
        Normal,
        Fade,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgInventory = settings.createGroup("Inventory");
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgTrapBreak = settings.createGroup("Trap Break");
    private final SettingGroup sgHold = settings.createGroup("Hold");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgChainPop = settings.createGroup("Chain Pop");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");


    // General
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("Range in which to target players.")
            .defaultValue(9)
            .min(0)
            .sliderRange(0, 16)
            .build()
    );

    public final Setting<Double> placeRadius = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-radius")
            .description("Max bed explosion radius to target.")
            .defaultValue(5)
            .range(1,10)
            .sliderRange(1,10)
            .build()
    );

    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder()
            .name("predict-movement")
            .description("Predicts target movement.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-terrain")
            .description("Completely ignores terrain if it can be blown up by beds.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fullBlocks = sgGeneral.add(new BoolSetting.Builder()
            .name("full-blocks")
            .description("Treat anvils and ender chests as full blocks.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> strictRotation = sgGeneral.add(new BoolSetting.Builder()
            .name("strict-rotation")
            .description("Rotates server-side towards the beds being broken/placed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<YawStepMode> yawStepMode = sgGeneral.add(new EnumSetting.Builder<YawStepMode>()
            .name("yaw-steps-mode")
            .description("When to run the yaw steps check.")
            .defaultValue(YawStepMode.Break)
            .visible(strictRotation::get)
            .build()
    );

    private final Setting<Double> yawSteps = sgGeneral.add(new DoubleSetting.Builder()
            .name("yaw-steps")
            .description("Maximum number of degrees its allowed to rotate in one tick.")
            .defaultValue(180)
            .range(1, 180)
            .sliderRange(1, 180)
            .visible(strictRotation::get)
            .build()
    );

    public final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug-mode")
            .description("Informs you about what the BA is doing.")
            .defaultValue(false)
            .build()
    );


    // Inventory
    private final Setting<Boolean> autoMove = sgInventory.add(new BoolSetting.Builder()
            .name("auto-move")
            .description("Moves beds into a selected hotbar slot.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Integer> autoMoveSlot = sgInventory.add(new IntSetting.Builder()
            .name("auto-move-slot")
            .description("The slot to move beds to.")
            .defaultValue(9)
            .range(1, 9)
            .sliderRange(1, 9)
            .visible(autoMove::get)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgInventory.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to and from beds automatically.")
            .defaultValue(true)
            .build());


    // Place
    private final Setting<Boolean> doPlace = sgPlace.add(new BoolSetting.Builder()
            .name("place")
            .description("If the BA should place beds.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> packetPlace = sgPlace.add(new BoolSetting.Builder()
            .name("packet-place")
            .description("If the BA should use packets to place beds.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> placeSwing = sgPlace.add(new BoolSetting.Builder()
            .name("place-swing")
            .description("Renders place hand swings.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ghostPlace = sgPlace.add(new BoolSetting.Builder()
            .name("ghost-place")
            .description("Hides hand swing for placing beds.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> selfPlaceExplosionRadius = sgPlace.add(new DoubleSetting.Builder()
            .name("self-place-explosion-radius")
            .description("Max bed explosion radius to self to calculate when placing.")
            .defaultValue(5)
            .range(1, 10)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<Double> PminDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("min-place-damage")
            .description("Minimum place damage the beds needs to deal to your target.")
            .defaultValue(10)
            .min(0)
            .sliderMax(36)
            .build()
    );

    public final Setting<DamageIgnore> PDamageIgnore = sgPlace.add(new EnumSetting.Builder<DamageIgnore>()
            .name("ignore-self-place-damage")
            .description("When to ignore self damage when placing beds.")
            .defaultValue(DamageIgnore.Never)
            .build()
    );

    private final Setting<Double> PmaxDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("max-place-damage")
            .description("Maximum place damage beds can deal to yourself.")
            .defaultValue(6)
            .range(0, 36)
            .sliderRange(0, 36)
            .visible(() -> PDamageIgnore.get() != DamageIgnore.Always)
            .build()
    );

    private final Setting<Boolean> PantiSuicide = sgPlace.add(new BoolSetting.Builder()
            .name("anti-suicide-place")
            .description("Will not place beds if they will pop / kill you.")
            .defaultValue(true)
            .visible(() -> PDamageIgnore.get() != DamageIgnore.Always)
            .build()
    );

    public final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
            .name("place-delay")
            .description("The delay in ticks to wait to place a bed.")
            .defaultValue(8)
            .min(0)
            .sliderRange(0, 20)
            .build()
    );

    public final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("Range in which to place beds.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-walls-range")
            .description("Range in which to place beds when behind blocks.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<Double> placeVerticalRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-vertical-range")
            .description("Vertical range in which to place beds.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );


    // Trap break
    public final Setting<Boolean> trapBreak = sgTrapBreak.add(new BoolSetting.Builder()
            .name("trap-break")
            .description("Attempts to place beds into target's hitbox when they are fully covered.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> antiSelf = sgTrapBreak.add(new BoolSetting.Builder()
            .name("anti-self")
            .description("Will not try to trap break if the you and the target are at the same position.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Integer> trapBreakDuration = sgTrapBreak.add(new IntSetting.Builder()
            .name("duration")
            .description("Amount of ticks it will allow you to perform this.")
            .defaultValue(10)
            .range(5, 35)
            .sliderRange(5, 35)
            .build()
    );

    // Hold

    public final Setting<Boolean> hold = sgHold.add(new BoolSetting.Builder()
            .name("hold")
            .description("Break beds slower to hold on to their surround when target is in hole.")
            .defaultValue(false)
            .build()
    );

    public final Setting<CommonEnums.ConTypeInclAlways> holdWhen = sgHold.add(new EnumSetting.Builder<CommonEnums.ConTypeInclAlways>()
            .name("hold-when")
            .description("When to start trap holding.")
            .defaultValue(CommonEnums.ConTypeInclAlways.AnyTrapped)
            .visible(hold::get)
            .build()
    );

    public final Setting<Integer> holdDelay = sgHold.add(new IntSetting.Builder()
            .name("hold-delay")
            .description("The delay in ticks to wait to break a bed for holding.")
            .defaultValue(8)
            .min(0)
            .sliderRange(0, 15)
            .visible(hold::get)
            .build()
    );


    // Break
    private final Setting<Boolean> doBreak = sgBreak.add(new BoolSetting.Builder()
            .name("break")
            .description("If the BA should break beds.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> selfBreakExplosionRadius = sgBreak.add(new DoubleSetting.Builder()
            .name("self-break-explosion-radius")
            .description("Max beds explosion radius to self to calculate when breaking.")
            .defaultValue(5)
            .range(1, 10)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<Double> BminDamage = sgBreak.add(new DoubleSetting.Builder()
            .name("min-break-damage")
            .description("Minimum break damage the beds needs to deal to your target.")
            .defaultValue(3)
            .min(0)
            .build()
    );

    public final Setting<DamageIgnore> BDamageIgnore = sgBreak.add(new EnumSetting.Builder<DamageIgnore>()
            .name("ignore-self-break-damage")
            .description("When to ignore self damage when breaking beds.")
            .defaultValue(DamageIgnore.Never)
            .build()
    );

    private final Setting<Double> BmaxDamage = sgBreak.add(new DoubleSetting.Builder()
            .name("max-break-damage")
            .description("Maximum break damage beds can deal to yourself.")
            .defaultValue(6)
            .range(0, 36)
            .sliderRange(0, 36)
            .visible(() -> BDamageIgnore.get() != DamageIgnore.Always)
            .build()
    );

    private final Setting<Boolean> BantiSuicide = sgBreak.add(new BoolSetting.Builder()
            .name("anti-suicide-break")
            .description("Will not break beds if they will pop / kill you.")
            .defaultValue(true)
            .visible(() -> BDamageIgnore.get() != DamageIgnore.Always)
            .build()
    );

    public final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
            .name("break-delay")
            .description("The delay in ticks to wait to break a bed.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 20)
            .build()
    );

    private final Setting<Boolean> smartDelay = sgBreak.add(new BoolSetting.Builder()
            .name("smart-delay")
            .description("Only breaks beds when the target can receive damage.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
            .name("break-range")
            .description("Range in which to break beds.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder()
            .name("break-walls-range")
            .description("Range in which to break beds when behind blocks.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build()
    );


    // Chain Pop
    public final Setting<Boolean> selfPopInvincibility = sgChainPop.add(new BoolSetting.Builder()
            .name("self-pop-invincibility")
            .description("Ignores self damage if you just popped.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Integer> selfPopInvincibilityTime = sgChainPop.add(new IntSetting.Builder()
            .name("self-pop-time")
            .description("How many millisecond to consider for self-pop invincibility")
            .defaultValue(300)
            .sliderRange(1, 2000)
            .visible(selfPopInvincibility::get)
            .build()
    );

    public final Setting<SelfPopIgnore> selfPopIgnore = sgChainPop.add(new EnumSetting.Builder<SelfPopIgnore>()
            .name("self-pop-ignore")
            .description("What to ignore when you just popped.")
            .defaultValue(SelfPopIgnore.Break)
            .visible(selfPopInvincibility::get)
            .build()
    );

    public final Setting<Boolean> targetPopInvincibility = sgChainPop.add(new BoolSetting.Builder()
            .name("target-pop-invincibility")
            .description("Tries to pause certain actions when your enemy just popped.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Integer> targetPopInvincibilityTime = sgChainPop.add(new IntSetting.Builder()
            .name("target-pop-time")
            .description("How many milliseconds to consider for target-pop invincibility")
            .defaultValue(500)
            .sliderRange(1, 2000)
            .visible(targetPopInvincibility::get)
            .build()
    );

    public final Setting<PopPause> popPause = sgChainPop.add(new EnumSetting.Builder<PopPause>()
            .name("pop-pause-mode")
            .description("What to pause when your enemy just popped.")
            .defaultValue(PopPause.Break)
            .visible(targetPopInvincibility::get)
            .build()
    );


    // Pause
    private final Setting<Double> pauseAtHealth = sgPause.add(new DoubleSetting.Builder()
            .name("pause-health")
            .description("Pauses when you go below a certain health.")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses BA when eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses BA when drinking.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Pauses BA when mining.")
            .defaultValue(false)
            .build()
    );


    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render-swing")
            .description("Whether to swing your hand while placing. (Needs to be implemented)")
            .defaultValue(true)
            .build()
    );

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(RenderMode.Normal)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(render::get)
            .build()
    );

    private final Setting<Boolean> detailedRender = sgRender.add(new BoolSetting.Builder()
            .name("detailed-render")
            .description("Whether or not to render the shape of an actual bed.")
            .defaultValue(false)
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255, 45))
            .visible(() -> render.get() && shapeMode.get() != ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> render.get() && shapeMode.get() != ShapeMode.Sides)
            .build()
    );

    private final Setting<Boolean> renderDamageText = sgRender.add(new BoolSetting.Builder()
            .name("damage")
            .description("Renders bed damage text in the block overlay.")
            .defaultValue(true)
            .visible(render::get)
            .build()
    );

    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder()
            .name("damage-scale")
            .description("How big the damage text should be.")
            .defaultValue(1.25)
            .min(1)
            .sliderMax(4)
            .visible(() -> render.get() && renderDamageText.get())
            .build()
    );

    private final Setting<SettingColor> damageTextColor = sgRender.add(new ColorSetting.Builder()
            .name("damage-color")
            .description("What the color of the damage text should be.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> render.get() && renderDamageText.get())
            .build()
    );

    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder()
            .name("place-time")
            .description("How long to render for.")
            .defaultValue(10)
            .min(0)
            .sliderMax(20)
            .visible(render::get)
            .build()
    );

    private final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder()
            .name("break")
            .description("Renders a block overlay where the block the beds are broken on.")
            .defaultValue(false)
            .build()
    );

    private final Setting<SettingColor> sideColorB = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255, 45))
            .visible(() -> renderBreak.get() && shapeMode.get() != ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> lineColorB = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> renderBreak.get() && shapeMode.get() != ShapeMode.Sides)
            .build()
    );

    private final Setting<Integer> renderBreakTime = sgRender.add(new IntSetting.Builder()
            .name("break-time")
            .description("How long to render breaking for.")
            .defaultValue(13)
            .min(0)
            .sliderMax(20)
            .visible(renderBreak::get)
            .build()
    );


    public MonkeSleeper() {
        super(BananaPlus.COMBAT, "monke-sleeper", "Automatically places and breaks beds.");
    }


    public int breakTimer;
    private int placeTimer;
    public int trapBreakTimer;

    public final List<PlayerEntity> targets = new ArrayList<>();

    private final Vec3d vec3d = new Vec3d(0, 0, 0);
    private final Vec3d playerEyePos = new Vec3d(0, 0, 0);
    private final Vec3 vec3 = new Vec3();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    public final BlockPos.Mutable placingBedBlockPos = new BlockPos.Mutable();

    private final Vec3d vec3dRayTraceEnd = new Vec3d(0, 0, 0);
    private RaycastContext raycastContext;

    private CardinalDirection direction;
    private double directedYaw;
    private double serverYaw;

    public PlayerEntity bestTarget;
    private float bestTargetDamage;
    private int bestTargetTimer;

    private boolean didRotateThisTick;
    private boolean isLastRotationPos;
    private final Vec3d lastRotationPos = new Vec3d(0, 0, 0);
    private double lastYaw, lastPitch;
    private int lastRotationTimer;

    public TimerUtils selfPoppedTimer = new TimerUtils();
    public TimerUtils targetPoppedTimer = new TimerUtils();

    private int renderTimer, breakRenderTimer;
    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();
    private final BlockPos.Mutable breakRenderPos = new BlockPos.Mutable();

    private int xOffset, zOffset;
    private double renderDamage;


    @Override
    public void onActivate() {
        breakTimer = 0;
        placeTimer = 0;
        trapBreakTimer = 0;

        raycastContext = new RaycastContext(new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        serverYaw = mc.player.getYaw();

        bestTargetDamage = 0;
        bestTargetTimer = 0;

        lastRotationTimer = getLastRotationStopDelay();

        renderTimer = 0;
        breakRenderTimer = 0;
    }

    @Override
    public void onDeactivate() {
        targets.clear();

        bestTarget = null;
    }

    private int getLastRotationStopDelay() {
        return Math.max(10, placeDelay.get() / 2 + breakDelay.get() / 2 + 10);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPreTick(TickEvent.Pre event) {
        if (mc.world.getDimension().isBedWorking()) {
            error("Bed no boom boom in this dimension... disabling.");
            toggle();
            return;
        }

        // Update last rotation
        didRotateThisTick = false;
        lastRotationTimer++;

        // Decrement best target timer
        if (bestTargetTimer > 0) bestTargetTimer--;
        bestTargetDamage = 0;

        // Decrement break, place and switch timers
        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
        if (trapBreakTimer > 0) trapBreakTimer--;

        // Decrement render timers
        if (renderTimer > 0) renderTimer--;
        if (breakRenderTimer > 0) breakRenderTimer--;

        // Check pause settings
        if (PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get()) || PlayerUtils.getTotalHealth() <= pauseAtHealth.get()) {
            if (debug.get()) warning("Pausing");
            return;
        }

        // Set player eye pos
        ((IVec3d) playerEyePos).set(mc.player.getPos().x, mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getPos().z);

        // Find targets, break and place
        findTargets();

        if (targets.size() > 0) {
            doPlace();
            doBreak();
        }

        if (autoMove.get()) BedUtil.doMove();
    }

    @EventHandler(priority = EventPriority.LOWEST - 666)
    private void onPreTickLast(TickEvent.Pre event) {
        // Rotate to last rotation
        if (strictRotation.get() && lastRotationTimer < getLastRotationStopDelay() && !didRotateThisTick) {
            Rotations.rotate(isLastRotationPos ? Rotations.getYaw(lastRotationPos) : lastYaw, isLastRotationPos ? Rotations.getPitch(lastRotationPos) : lastPitch, -100, null);
        }
    }

    private void setRotation(boolean isPos, Vec3d pos, double yaw, double pitch) {
        didRotateThisTick = true;
        isLastRotationPos = isPos;

        if (isPos) ((IVec3d) lastRotationPos).set(pos.x, pos.y, pos.z);
        else {
            lastYaw = yaw;
            lastPitch = pitch;
        }

        lastRotationTimer = 0;
    }

    // Break

    private void doBreak() {
        if (!doBreak.get() || breakTimer > 0 || (popPause.get() != PopPause.Place && BedUtil.targetJustPopped()))
            return;

        BlockPos bedPos = getBreakPosition();

        if (bedPos != null) {
            // Rotate and attack
            boolean attacked = true;

            if (!strictRotation.get()) {
                BedUtil.breakBed(bedPos);
            } else {
                Vec3d bedVec = new Vec3d(bedPos.getX() + 0.5, bedPos.getY() + 0.28125, bedPos.getZ() + 0.5);

                double yaw = Rotations.getYaw(bedVec);
                double pitch = Rotations.getPitch(bedVec);

                if (doYawSteps(yaw, pitch)) {
                    setRotation(true, bedVec, 0, 0);
                    Rotations.rotate(yaw, pitch, 50, () -> BedUtil.breakBed(bedPos));
                } else {
                    attacked = false;
                }
            }

            if (attacked) {

                // Break render
                breakRenderPos.set(bedPos);
                breakRenderTimer = renderBreakTime.get();
            }
        }
    }

    private BlockPos getBreakPosition() {
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (blockEntity instanceof BedBlockEntity) {

                BlockPos bedPos = blockEntity.getPos();
                BlockPos damagePos = bedPos;

                Vec3d bedVec = Vec3d.ofCenter(bedPos);
                Vec3d damageVec = Vec3d.ofCenter(damagePos);

                if (!isOutOfRange(bedVec, false)) {
                    // Check if it's the foot of the bed, if it is we offset the position to the head
                    if (mc.world.getBlockState(bedPos).get(Properties.BED_PART).equals(BedPart.FOOT)) {
                        damagePos.offset(mc.world.getBlockState(bedPos).get(Properties.HORIZONTAL_FACING));
                    }

                    if (!BedUtil.shouldIgnoreSelfBreakDamage()) {
                        float selfDamage = BPlusDamageUtils.bedDamage(mc.player, damageVec, predictMovement.get(), selfBreakExplosionRadius.get().floatValue(), ignoreTerrain.get(), fullBlocks.get());
                        if (selfDamage > BmaxDamage.get() || (BantiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)))
                            continue;
                    } else if (debug.get()) warning("Ignoring self break dmg");

                    // Check damage to targets
                    float damage = getDamageToTargets(damageVec, true, false);

                    if (damage < BminDamage.get()) continue;

                    return bedPos;
                }
            }
        }

        return null;
    }

    /*
    private float getBreakDamage(BlockEntity entity) {
        if (!(entity instanceof BedBlockEntity)) return 0;
        BlockPos bedPos = entity.getPos();
        BlockPos damagePos = bedPos;
        // Check if it's the foot of the bed, if it is we offset the position to the head
        if (mc.world.getBlockState(bedPos).get(Properties.BED_PART).equals(BedPart.FOOT)) {
            damagePos.offset(mc.world.getBlockState(bedPos).get(Properties.HORIZONTAL_FACING));
        }
        Vec3d bedVec = Vec3d.ofCenter(bedPos);
        Vec3d damageVec = Vec3d.ofCenter(damagePos);
        // Check range
        if (isOutOfRange(bedVec, false)) return 0;
        if (!BedUtil.shouldIgnoreSelfBreakDamage()) {
            float selfDamage = BPlusDamageUtils.bedDamage(mc.player, damageVec, predictMovement.get(), selfBreakExplosionRadius.get().floatValue(), ignoreTerrain.get(), fullAnvil.get(), fullEchest.get());
            if (selfDamage > BmaxDamage.get() || (BantiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)))
                return 0;
        } else if (debug.get()) warning("Ignoring self break dmg");
        // Check damage to targets
        float damage = getDamageToTargets(damageVec, true, false);
        if (damage < BminDamage.get()) return 0;
        return damage;
    }
     */

    private final List<BlockPos> trapBreakPositions = new ArrayList<>();

    private void doTrapBreak() {
        if (!doPlace.get()) return;

        // Return if there are no beds in inventory
        if (!InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).found()) return;

        findPlacePos(bestTarget);

        BlockPos blockPos = trapBreakPositions.get(trapBreakPositions.size() - 1);

        if (BPlusWorldUtils.place(blockPos, InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem), false, 50, BPlusWorldUtils.SwitchMode.Client, BPlusWorldUtils.PlaceMode.Packet, false, BPlusWorldUtils.AirPlaceDirection.Up, placeSwing.get(), true, true))
            trapBreakPositions.remove(blockPos);

    }

    private void findPlacePos(PlayerEntity target) {
        trapBreakPositions.clear();
        BlockPos targetPos = BPlusEntityUtils.playerPos(bestTarget);

        // Feet positions
        trapBreakPositions.add(targetPos.add(1, 0, 0));
        trapBreakPositions.add(targetPos.add(0, 0, 1));
        trapBreakPositions.add(targetPos.add(-1, 0, 0));
        trapBreakPositions.add(targetPos.add(0, 0, -1));

        // Head positions
        trapBreakPositions.add(targetPos.add(1, 1, 0));
        trapBreakPositions.add(targetPos.add(0, 1, 1));
        trapBreakPositions.add(targetPos.add(-1, 1, 0));
        trapBreakPositions.add(targetPos.add(0, 1, -1));
    }

    // Place

    private void doPlace() {
        if (!doPlace.get() || placeTimer > 0 || (popPause.get() != PopPause.Break && BedUtil.targetJustPopped()))
            return;

        // Return if there are no beds in inventory
        if (!InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).found()) return;

        // Check for multiplace
        if (getBreakPosition() != null) return;

        // Setup variables
        AtomicDouble bestDamage = new AtomicDouble(0);
        AtomicReference<BlockPos.Mutable> bestBlockPos = new AtomicReference<>(new BlockPos.Mutable());

        // Find best position to place the bed on
        BlockIterator.register((int) Math.ceil(placeRange.get()), (int) Math.ceil(placeVerticalRange.get()), (bp, blockState) -> {

            boolean isBedHead = blockState.getBlock() instanceof BedBlock && blockState.get(Properties.BED_PART).equals(BedPart.HEAD);

            // Check if it is air
            if (!blockState.getMaterial().isReplaceable() && !isBedHead) return;

            // Check range
            ((IVec3d) vec3d).set(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5);
            if (isOutOfRange(vec3d, true)) return;

            if (BedUtil.getClosestOffset(bp) == null) return;

            // Check damage to self and anti suicide
            if (!BedUtil.shouldIgnoreSelfPlaceDamage()) {
                float selfDamage = BPlusDamageUtils.bedDamage(mc.player, vec3d, predictMovement.get(), selfPlaceExplosionRadius.get().floatValue(), ignoreTerrain.get(), fullBlocks.get());
                if (selfDamage > PmaxDamage.get() || (PantiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)))
                    return;
            } else if (debug.get()) warning("Ignoring self place dmg");

            // Check damage to targets and face place
            float damage = getDamageToTargets(vec3d, false, false);

            if (damage < PminDamage.get()) return;

            // Compare damage
            if (damage > bestDamage.get()) {
                bestDamage.set(damage);
                bestBlockPos.get().set(bp);
            }

        });

        // Place the bed
        BlockIterator.after(() -> {
            if (bestDamage.get() == 0) return;

            BlockHitResult result = getPlaceInfo(BedUtil.getClosestOffset(bestBlockPos.get()));

            direction = BedUtil.offsetDirection(result.getBlockPos(), bestBlockPos.get());
            if (debug.get()) {
                switch (direction) {
                    case North -> warning("north");
                    case East -> warning("east");
                    case South -> warning("south");
                    case West -> warning("west");
                }
            }

            ((IVec3d) vec3d).set(
                    result.getBlockPos().getX() + 0.5 + result.getSide().getVector().getX() * 0.5,
                    result.getBlockPos().getY() + 0.5 + result.getSide().getVector().getY() * 0.5,
                    result.getBlockPos().getZ() + 0.5 + result.getSide().getVector().getZ() * 0.5
            );

            if (strictRotation.get()) {
                double yaw = Rotations.getYaw(vec3d);
                double pitch = Rotations.getPitch(vec3d);

                if (yawStepMode.get() == YawStepMode.Break || doYawSteps(yaw, pitch)) {
                    setRotation(true, vec3d, 0, 0);
                    Rotations.rotate(yaw, pitch, 50, () -> placeBed(result, bestDamage.get()));

                    placeTimer = placeDelay.get();
                }
            } else {
                Rotations.rotate(BedUtil.getDirectedYaw(result.getBlockPos(), bestBlockPos.get()), mc.player.getPitch(), 50, () -> placeBed(result, bestDamage.get()));

                placeTimer = placeDelay.get();
            }
        });
    }

    private BlockHitResult getPlaceInfo(BlockPos blockPos) {
        ((IVec3d) vec3d).set(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        for (Direction side : Direction.values()) {
            ((IVec3d) vec3dRayTraceEnd).set(
                    blockPos.getX() + 0.5 + side.getVector().getX() * 0.5,
                    blockPos.getY() + 0.5 + side.getVector().getY() * 0.5,
                    blockPos.getZ() + 0.5 + side.getVector().getZ() * 0.5
            );

            ((IRaycastContext) raycastContext).set(vec3d, vec3dRayTraceEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);

            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                return result;
            }
        }

        Direction side = blockPos.getY() > vec3d.y ? Direction.DOWN : Direction.UP;
        return new BlockHitResult(vec3d, side, blockPos, false);
    }

    private void placeBed(BlockHitResult result, double damage) {
        // Switch
        FindItemResult bed = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (!bed.found()) return;

        int prevSlot = mc.player.getInventory().selectedSlot;

        if (autoSwitch.get() && !bed.isOffhand()) InvUtils.swap(bed.slot(), false);

        Hand hand = bed.getHand();
        if (hand == null) return;

        // Place bed
        if (packetPlace.get()) mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, result));
        else mc.interactionManager.interactBlock(mc.player, mc.world, hand, result);

        if (placeSwing.get()) mc.player.swingHand(hand);
        if (!ghostPlace.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        // placingBedBlockPos.set(result.getBlockPos());
        // Insta break beds
        if (breakDelay.get() == 0) BedUtil.breakBed(result.getBlockPos());

        if (debug.get()) warning("Placing");

        renderTimer = renderTime.get();
        renderPos.set(result.getBlockPos());
        renderDamage = damage;

        // Switch back
        if (autoSwitch.get()) InvUtils.swap(prevSlot, false);
    }

    // Yaw steps

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            serverYaw = ((PlayerMoveC2SPacket) event.packet).getYaw((float) serverYaw);
        }
    }

    public boolean doYawSteps(double targetYaw, double targetPitch) {
        targetYaw = MathHelper.wrapDegrees(targetYaw) + 180;
        double serverYaw = MathHelper.wrapDegrees(this.serverYaw) + 180;

        if (distanceBetweenAngles(serverYaw, targetYaw) <= yawSteps.get()) return true;

        double delta = Math.abs(targetYaw - serverYaw);
        double yaw = this.serverYaw;

        if (serverYaw < targetYaw) {
            if (delta < 180) yaw += yawSteps.get();
            else yaw -= yawSteps.get();
        } else {
            if (delta < 180) yaw -= yawSteps.get();
            else yaw += yawSteps.get();
        }

        setRotation(false, null, yaw, targetPitch);
        Rotations.rotate(yaw, targetPitch, -100, null); // Priority -100 so it sends the packet as the last one, im pretty sure it doesn't matte but idc
        return false;
    }

    private static double distanceBetweenAngles(double alpha, double beta) {
        double phi = Math.abs(beta - alpha) % 360;
        return phi > 180 ? 360 - phi : phi;
    }

    // Others

    private boolean isOutOfRange(Vec3d vec3d, boolean place) {
        ((IRaycastContext) raycastContext).set(playerEyePos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        BlockHitResult result = mc.world.raycast(raycastContext);
        boolean behindWall = result == null;
        double distance = mc.player.getEyePos().distanceTo(vec3d);

        return distance > (behindWall ? (place ? placeWallsRange : breakWallsRange).get() : (place ? placeRange : breakRange).get());
    }

    private PlayerEntity getNearestTarget() {
        PlayerEntity nearestTarget = null;
        double nearestDistance = targetRange.get();

        for (PlayerEntity target : targets) {
            double distance = target.squaredDistanceTo(mc.player);

            if (distance < nearestDistance) {
                nearestTarget = target;
                nearestDistance = distance;
            }
        }

        return nearestTarget;
    }

    private float getDamageToTargets(Vec3d vec3d, boolean breaking, boolean fast) {
        float damage = 0;

        if (fast) {
            PlayerEntity target = getNearestTarget();
            if (!(smartDelay.get() && breaking && target.hurtTime > 0))
                damage = BPlusDamageUtils.bedDamage(target, vec3d, predictMovement.get(), placeRadius.get().floatValue(), ignoreTerrain.get(), fullBlocks.get());
        } else {
            for (PlayerEntity target : targets) {
                if (smartDelay.get() && breaking && target.hurtTime > 0) continue;

                float dmg = BPlusDamageUtils.bedDamage(target, vec3d, predictMovement.get(), placeRadius.get().floatValue(), ignoreTerrain.get(), fullBlocks.get());

                // Update best target
                if (dmg > bestTargetDamage) {
                    bestTarget = target;
                    bestTargetDamage = dmg;
                    bestTargetTimer = 10;
                }

                damage += dmg;
            }
        }

        return damage;
    }

    @Override
    public String getInfoString() {
        return bestTarget != null && bestTargetTimer > 0 ? bestTarget.getGameProfile().getName() : null;
    }

    private void findTargets() {
        targets.clear();

        // Players
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getAbilities().creativeMode || player == mc.player) continue;

            if (!player.isDead() && player.isAlive() && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                targets.add(player);
            }
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;

        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);

        if (!(entity instanceof PlayerEntity)) return;

        if (entity.equals(mc.player) && selfPopInvincibility.get()) selfPoppedTimer.reset();

        if (entity.equals(bestTarget) && targetPopInvincibility.get()) targetPoppedTimer.reset();

    }

    @EventHandler(priority = EventPriority.LOWEST - 1000)
    private void onTick(TickEvent.Post event) {
        if (debug.get()) {
            if (CrystalUtils.isSurroundHolding()) warning("Surround holding");
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (renderMode.get() != RenderMode.None && renderTimer > 0) {

            int x = renderPos.getX();
            int y = renderPos.getY();
            int z = renderPos.getZ();

            switch (direction) {
                case North -> zOffset = 0;
                case South -> zOffset = 1;
                //case East ->
                //case West ->
            }

            if (!detailedRender.get()) {
                switch (direction) {
                    case North -> event.renderer.box(x, y, z - 1, x + 1, y + 0.5625, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    case South -> event.renderer.box(x, y, z, x + 1, y + 0.5625, z + 2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    case East -> event.renderer.box(x, y, z, x + 2, y + 0.5625, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    case West -> event.renderer.box(x - 1, y, z, x + 1, y + 0.5625, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }

            if (detailedRender.get()) {
                switch (direction) {
                    case North, South -> {
                        // Body
                        event.renderer.box(x + 0.1875, y + 0.1875, z - 0.8125 + zOffset, x + 0.8125, y + 0.5625, z + 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), -7);

                        // Left and Right
                        event.renderer.box(x, y + 0.1875, z + 0.8125 + zOffset, x + 0.1875, y + 0.5625, z - 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), -39);
                        event.renderer.box(x + 0.8125, y + 0.1875, z + 0.8125 + zOffset, x + 1, y + 0.5625, z - 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), 56);

                        // Front and Back
                        event.renderer.box(x + 0.1875, y + 0.1875, z + 0.8125 + zOffset, x + 0.8125, y + 0.5625, z + 1 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), 104);
                        event.renderer.box(x + 0.1875, y + 0.1875, z - 0.8125 + zOffset, x + 0.8125, y + 0.5625, z - 1 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), 104);

                        // Legs
                        event.renderer.box(x, y, z + 1 + zOffset, x + 0.1875, y + 0.1875, z + 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), 2);
                        event.renderer.box(x, y, z - 1 + zOffset, x + 0.1875, y + 0.1875, z - 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), 2);
                        event.renderer.box(x + 0.8125, y, z + 1 + zOffset, x + 1, y + 0.1875, z + 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), 2);
                        event.renderer.box(x + 1, y, z - 1 + zOffset, x + 0.8125, y + 0.1875, z - 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), 2);

                        // Corners
                        event.renderer.box(x, y + 0.1875, z + 1 + zOffset, x + 0.1875, y + 0.5625, z + 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), -44);
                        event.renderer.box(x, y + 0.1875, z - 1 + zOffset, x + 0.1875, y + 0.5625, z - 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), 84);
                        event.renderer.box(x + 0.8125, y + 0.1875, z + 1 + zOffset, x + 1, y + 0.5625, z + 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), 52);
                        event.renderer.box(x + 1, y + 0.1875, z - 1 + zOffset, x + 0.8125, y + 0.5625, z - 0.8125 + zOffset, sideColor.get(), lineColor.get(), shapeMode.get(), 84);

                    }

                    case East, West -> {
                        event.renderer.box(x, y, z, x + 2, y + 0.5625, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                }
            }
        }
    }


    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!render.get() || renderTimer <= 0 || !renderDamageText.get()) return;

        switch (direction) {
            case North -> vec3.set(renderPos.getX() + 0.5, renderPos.getY() + (detailedRender.get() ? 0.435 : 0.28125), renderPos.getZ());
            case East -> vec3.set(renderPos.getX() + 1, renderPos.getY() + (detailedRender.get() ? 0.435 : 0.28125), renderPos.getZ() + 0.5);
            case South -> vec3.set(renderPos.getX() + 0.5, renderPos.getY() + (detailedRender.get() ? 0.435 : 0.28125), renderPos.getZ() + 1);
            case West -> vec3.set(renderPos.getX(), renderPos.getY() + (detailedRender.get() ? 0.435 : 0.28125), renderPos.getZ() + 0.5);
        }

        if (NametagUtils.to2D(vec3, damageTextScale.get())) {
            NametagUtils.begin(vec3);
            TextRenderer.get().begin(1, false, true);

            String text = String.format("%.1f", renderDamage);
            double w = TextRenderer.get().getWidth(text) * 0.5;
            TextRenderer.get().render(text, -w, 0, damageTextColor.get(), true);

            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    public PlayerEntity getPlayerTarget () {
        if (bestTarget != null) {
            return bestTarget;
        } else {
            return null;
        }
    }
}
