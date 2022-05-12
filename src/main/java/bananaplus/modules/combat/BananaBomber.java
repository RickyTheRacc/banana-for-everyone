package bananaplus.modules.combat;

import bananaplus.modules.AddModule;
import bananaplus.utils.*;
import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.ints.*;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BananaBomber extends Module {
    public enum YawStepMode {
        Break,
        All,
    }

    public enum AutoSwitchMode {
        Normal,
        Silent,
        None
    }

    public enum SupportMode {
        Disabled,
        Accurate,
        Fast
    }

    public enum CancelCrystalMode {
        Hit,
        NoDesync
    }

    public enum DamageIgnore {
        Always,
        SurroundedorBurrowed,
        Never
    }

    public enum SlowMode {
        Delay,
        Age,
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
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgFacePlace = settings.createGroup("Face Place");
    private final SettingGroup sgSurround = settings.createGroup("Surround");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgFastBreak = settings.createGroup("Fast Break");
    private final SettingGroup sgChainPop = settings.createGroup("Chain Pop");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    public final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug-mode")
            .description("Informs you about what the CA is doing.")
            .defaultValue(false)
            .build());

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("Range in which to target players.")
            .defaultValue(10)
            .min(0)
            .sliderRange(0,18)
            .build());

    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder()
            .name("predict-movement")
            .description("Predicts target movement.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-terrain")
            .description("Completely ignores terrain if it can be blown up by end crystals.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> fullAnvil = sgGeneral.add(new BoolSetting.Builder()
            .name("full-anvil")
            .description("Completely ignores gaps between anvil blocks for damage calc.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> fullEchest = sgGeneral.add(new BoolSetting.Builder()
            .name("full-E-Chest")
            .description("Completely ignores gaps between E-chest blocks for damage calc.")
            .defaultValue(false)
            .build());

    public final Setting<Double> explosionRadiusToTarget = sgGeneral.add(new DoubleSetting.Builder()
            .name("explosion-radius-to-target")
            .description("Max crystal explosion radius to target.")
            .defaultValue(12)
            .range(1,12)
            .sliderRange(1,12)
            .build());

    private final Setting<Integer> waiting = sgGeneral.add(new IntSetting.Builder()
            .name("min-explosion-time")
            .description("Min tick duration for crystal explosion")
            .defaultValue(3)
            .min(0)
            .sliderRange(0,6)
            .build());

    private final Setting<AutoSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>()
            .name("auto-switch")
            .description("Switches to crystals in your hotbar once a target is found.")
            .defaultValue(AutoSwitchMode.Normal)
            .build());

    private final Setting<Boolean> noGapSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("No Gap Switch")
            .description("Disables normal auto switch when you are holding a gap.")
            .defaultValue(true)
            .visible(() -> autoSwitch.get() == AutoSwitchMode.Normal)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates server-side towards the crystals being hit/placed.")
            .defaultValue(false)
            .build());

    private final Setting<YawStepMode> yawStepMode = sgGeneral.add(new EnumSetting.Builder<YawStepMode>()
            .name("yaw-steps-mode")
            .description("When to run the yaw steps check.")
            .defaultValue(YawStepMode.Break)
            .visible(rotate::get)
            .build());

    private final Setting<Double> yawSteps = sgGeneral.add(new DoubleSetting.Builder()
            .name("yaw-steps")
            .description("Maximum number of degrees its allowed to rotate in one tick.")
            .defaultValue(180)
            .range(1,180)
            .sliderRange(1,180)
            .visible(rotate::get)
            .build());

    // Place

    private final Setting<Boolean> doPlace = sgPlace.add(new BoolSetting.Builder()
            .name("place")
            .description("If the CA should place crystals.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> placeSwing = sgPlace.add(new BoolSetting.Builder()
            .name("place-swing")
            .description("Renders place hand swings.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> ghostPlace = sgPlace.add(new BoolSetting.Builder()
            .name("ghost-place")
            .description("Hides hand swing for placing crystals.")
            .defaultValue(false)
            .build());

    private final Setting<Double> selfPlaceExplosionRadius = sgPlace.add(new DoubleSetting.Builder()
            .name("self-place-explosion-radius")
            .description("Max crystal explosion radius to self to calculate when placing.")
            .defaultValue(12)
            .range(1,12)
            .sliderRange(1,12)
            .build());

    private final Setting<Double> PminDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("min-place-damage")
            .description("Minimum place damage the crystal needs to deal to your target.")
            .defaultValue(6)
            .min(0)
            .build());

    public final Setting<DamageIgnore> PDamageIgnore = sgPlace.add(new EnumSetting.Builder<DamageIgnore>()
            .name("ignore-self-place-damage")
            .description("When to ignore self damage when placing crystal.")
            .defaultValue(DamageIgnore.Never)
            .build());

    private final Setting<Double> PmaxDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("max-place-damage")
            .description("Maximum place damage crystals can deal to yourself.")
            .defaultValue(6)
            .range(0,36)
            .sliderRange(0,36)
            .visible(() -> PDamageIgnore.get() != DamageIgnore.Always)
            .build());

    private final Setting<Boolean> PantiSuicide = sgPlace.add(new BoolSetting.Builder()
            .name("anti-suicide-place")
            .description("Will not place crystals if they will pop / kill you.")
            .defaultValue(true)
            .visible(() -> PDamageIgnore.get() != DamageIgnore.Always)
            .build());

    public final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
            .name("place-delay")
            .description("The delay in ticks to wait to place a crystal after it's exploded.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0,20)
            .build());

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("Range in which to place crystals.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build());

    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-walls-range")
            .description("Range in which to place crystals when behind blocks.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build());

    private final Setting<Double> placeVerticalRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-vertical-range")
            .description("Vertical range in which to place crystals.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build());

    private final Setting<Boolean> placement112 = sgPlace.add(new BoolSetting.Builder()
            .name("1.12-placement")
            .description("Uses 1.12 crystal placement.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> smallBox = sgPlace.add(new BoolSetting.Builder()
            .name("small-box")
            .description("Allows you to place in 1x1x1 box instead of 1x2x1 boxes.")
            .defaultValue(false)
            .build());

    private final Setting<SupportMode> support = sgPlace.add(new EnumSetting.Builder<SupportMode>()
            .name("support")
            .description("Places a support block in air if no other position have been found.")
            .defaultValue(SupportMode.Disabled)
            .build());

    private final Setting<Integer> supportDelay = sgPlace.add(new IntSetting.Builder()
            .name("support-delay")
            .description("Delay in ticks after placing support block.")
            .defaultValue(1)
            .min(0)
            .visible(() -> support.get() != SupportMode.Disabled)
            .build());

    // Face place

    public final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder()
            .name("face-place")
            .description("Will face-place when target is below a certain health or armor durability threshold.")
            .defaultValue(true)
            .build());

    public final Setting<Boolean> slowFacePlace = sgFacePlace.add(new BoolSetting.Builder()
            .name("slow-face-place")
            .description("Will slow down face-place to save crystals.")
            .defaultValue(false)
            .build());

    public final Setting<SlowMode> slowFPMode = sgFacePlace.add(new EnumSetting.Builder<SlowMode>()
            .name("slow-FP-mode")
            .description("Timing to use for slow faceplace.")
            .defaultValue(SlowMode.Both)
            .visible(slowFacePlace::get)
            .build());

    public final Setting<Integer> slowFPDelay = sgFacePlace.add(new IntSetting.Builder()
            .name("slow-FP-delay")
            .description("The delay in ticks to wait to break a crystal for slow FP.")
            .defaultValue(10)
            .min(0)
            .sliderRange(0,15)
            .visible(() -> slowFacePlace.get() && slowFPMode.get() != SlowMode.Age)
            .build());

    public final Setting<Integer> slowFPAge = sgFacePlace.add(new IntSetting.Builder()
            .name("slow-FP-age")
            .description("Crystal age for slow faceplace (to prevent unnecessary attacks when people are around.")
            .defaultValue(3)
            .min(0)
            .sliderRange(0,15)
            .visible(() -> slowFacePlace.get() && slowFPMode.get() != SlowMode.Delay)
            .build());

    public final Setting<Boolean> surrHoldPause = sgFacePlace.add(new BoolSetting.Builder()
            .name("Pause-on-surround-hold")
            .description("Will pause face placing when surround hold is active.")
            .defaultValue(true)
            .build());

    public final Setting<Boolean> KAPause = sgFacePlace.add(new BoolSetting.Builder()
            .name("Pause-on-KA")
            .description("Will pause face placing when KA is active.")
            .defaultValue(true)
            .build());

    public final Setting<Boolean> CevPause = sgFacePlace.add(new BoolSetting.Builder()
            .name("Pause-on-Cev-Break")
            .description("Will pause face placing when Cev Breaker is active.")
            .defaultValue(true)
            .build());

    public final Setting<Boolean> greenHolers = sgFacePlace.add(new BoolSetting.Builder()
            .name("Green-holers")
            .description("Will automatically face-place when target's is in greenhole.")
            .defaultValue(false)
            .build());

    public final Setting<Boolean> faceSurrounded = sgFacePlace.add(new BoolSetting.Builder()
            .name("face-surrounded")
            .description("Will face-place even when target's face is surrounded.")
            .defaultValue(false)
            .build());

    public final Setting<Double> facePlaceHealth = sgFacePlace.add(new DoubleSetting.Builder()
            .name("face-place-health")
            .description("The health the target has to be at to start face placing.")
            .defaultValue(8)
            .min(0)
            .sliderRange(0,36)
            .build());

    public final Setting<Double> facePlaceDurability = sgFacePlace.add(new DoubleSetting.Builder()
            .name("face-place-durability")
            .description("The durability threshold percentage to be able to face-place.")
            .defaultValue(2)
            .min(0)
            .sliderRange(0,100)
            .build());

    public final Setting<Boolean> facePlaceArmor = sgFacePlace.add(new BoolSetting.Builder()
            .name("face-place-missing-armor")
            .description("Automatically starts face placing when a target misses a piece of armor.")
            .defaultValue(false)
            .build());

    public final Setting<Keybind> forceFacePlace = sgFacePlace.add(new KeybindSetting.Builder()
            .name("force-face-place")
            .description("Starts face place when this button is pressed.")
            .defaultValue(Keybind.none())
            .build());

    // Surround

    public final Setting<Boolean> burrowBreak = sgSurround.add(new BoolSetting.Builder()
            .name("burrow-break")
            .description("Will try to break target's burrow.")
            .defaultValue(false)
            .build());

    public final Setting<Keybind> forceBurrowBreak = sgSurround.add(new KeybindSetting.Builder()
            .name("force-burrow-break")
            .description("Starts burrow breaking when this button is pressed.")
            .defaultValue(Keybind.none())
            .build());

    public final Setting<Integer> burrowBreakDelay = sgSurround.add(new IntSetting.Builder()
            .name("burrow-break-delay")
            .description("Place delay in ticks for burrow break.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0,20)
            .build());

    public final Setting<ConTypeInclAlways> burrowBWhen = sgSurround.add(new EnumSetting.Builder<ConTypeInclAlways>()
            .name("burrow-break-when")
            .description("When to start burrow breaking.")
            .defaultValue(ConTypeInclAlways.Always)
            .build());

    public final Setting<Boolean> surroundBreak = sgSurround.add(new BoolSetting.Builder()
            .name("surround-break")
            .description("Will automatically places a crystal next to target's surround.")
            .defaultValue(false)
            .build());

    public final Setting<Keybind> forceSurroundBreak = sgSurround.add(new KeybindSetting.Builder()
            .name("force-surround-break")
            .description("Starts surround breaking when this button is pressed.")
            .defaultValue(Keybind.none())
            .build());

    public final Setting<ConTypeInclAlways> surroundBWhen = sgSurround.add(new EnumSetting.Builder<ConTypeInclAlways>()
            .name("surround-break-when")
            .description("When to start surround breaking.")
            .defaultValue(ConTypeInclAlways.FaceTrapped)
            .build());

    public final Setting<Integer> surroundBreakDelay = sgSurround.add(new IntSetting.Builder()
            .name("surround-break-delay")
            .description("Place delay in ticks for surround break.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0,20)
            .build());

    public final Setting<Boolean> surroundBHorse = sgSurround.add(new BoolSetting.Builder()
            .name("horse")
            .description("Allow horse sides of the target's surround to be surround broken.")
            .defaultValue(false)
            .build());

    public final Setting<Boolean> surroundBDiagonal = sgSurround.add(new BoolSetting.Builder()
            .name("diagonal")
            .description("Allow diagonal sides of the target's surround to be surround broken.")
            .defaultValue(false)
            .build());

    public final Setting<Boolean> surroundHold = sgSurround.add(new BoolSetting.Builder()
            .name("surround-hold")
            .description("Break crystals slower to hold on to their surround when their surround is broken.")
            .defaultValue(false)
            .build());

    public final Setting<ConTypeInclAlways> surroundHWhen = sgSurround.add(new EnumSetting.Builder<ConTypeInclAlways>()
            .name("surround-hold-when")
            .description("When to start surround holding.")
            .defaultValue(ConTypeInclAlways.AnyTrapped)
            .visible(surroundHold::get)
            .build());

    public final Setting<SlowMode> surroundHoldMode = sgSurround.add(new EnumSetting.Builder<SlowMode>()
            .name("surround-hold-mode")
            .description("Timing to use for surround hold.")
            .defaultValue(SlowMode.Both)
            .visible(surroundHold::get)
            .build());

    public final Setting<Integer> surroundHoldDelay = sgSurround.add(new IntSetting.Builder()
            .name("surround-hold-delay")
            .description("The delay in ticks to wait to break a crystal for surround hold.")
            .defaultValue(10)
            .min(0)
            .sliderRange(0,15)
            .visible(() -> surroundHold.get() && surroundHoldMode.get() != SlowMode.Age)
            .build());

    public final Setting<Integer> surroundHoldAge = sgSurround.add(new IntSetting.Builder()
            .name("surround-hold-age")
            .description("Crystal age for surround hold (to prevent unnecessary attacks when people are around.")
            .defaultValue(3)
            .min(0)
            .sliderRange(0,15)
            .visible(() -> surroundHold.get() && surroundHoldMode.get() != SlowMode.Delay)
            .build());

    // Break

    private final Setting<Boolean> doBreak = sgBreak.add(new BoolSetting.Builder()
            .name("break")
            .description("If the CA should break crystals.")
            .defaultValue(true)
            .build());

    public final Setting<Boolean> breakSwing = sgBreak.add(new BoolSetting.Builder()
            .name("break-swing")
            .description("Renders break hand swings.")
            .defaultValue(true)
            .build());

    public final Setting<Boolean> ghostBreak = sgBreak.add(new BoolSetting.Builder()
            .name("ghost-break")
            .description("Hide hand swings for breaking crystals.")
            .defaultValue(false)
            .build());

    private final Setting<Double> selfBreakExplosionRadius = sgBreak.add(new DoubleSetting.Builder()
            .name("self-break-explosion-radius")
            .description("Max crystal explosion radius to self to calculate when breaking.")
            .defaultValue(12)
            .range(1,12)
            .sliderRange(1,12)
            .build());

    public final Setting<Double> BminDamage = sgBreak.add(new DoubleSetting.Builder()
            .name("min-break-damage")
            .description("Minimum break damage the crystal needs to deal to your target.")
            .defaultValue(6)
            .min(0)
            .build());

    public final Setting<DamageIgnore> BDamageIgnore = sgBreak.add(new EnumSetting.Builder<DamageIgnore>()
            .name("ignore-self-break-damage")
            .description("When to ignore self damage when breaking crystal.")
            .defaultValue(DamageIgnore.Never)
            .build());

    private final Setting<Double> BmaxDamage = sgBreak.add(new DoubleSetting.Builder()
            .name("max-break-damage")
            .description("Maximum break damage crystals can deal to yourself.")
            .defaultValue(6)
            .range(0,36)
            .sliderRange(0,36)
            .visible(() -> BDamageIgnore.get() != DamageIgnore.Always)
            .build());

    private final Setting<Boolean> BantiSuicide = sgBreak.add(new BoolSetting.Builder()
            .name("anti-suicide-break")
            .description("Will not break crystals if they will pop / kill you.")
            .defaultValue(true)
            .visible(() -> BDamageIgnore.get() != DamageIgnore.Always)
            .build());

    public final Setting<Boolean> sneak = sgBreak.add(new BoolSetting.Builder()
            .name("sneak")
            .description("Should it do sneak while attacking crystals.")
            .defaultValue(false)
            .build());

    private final Setting<CancelCrystalMode> cancelCrystalMode = sgBreak.add(new EnumSetting.Builder<CancelCrystalMode>()
            .name("cancel-mode")
            .description("Mode to use for the crystals to be removed from the world.")
            .defaultValue(CancelCrystalMode.NoDesync)
            .build());

    public final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
            .name("break-delay")
            .description("The delay in ticks to wait to break a crystal after it's placed.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0,20)
            .build());

    private final Setting<Boolean> smartDelay = sgBreak.add(new BoolSetting.Builder()
            .name("smart-delay")
            .description("Only breaks crystals when the target can receive damage.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> switchDelay = sgBreak.add(new IntSetting.Builder()
            .name("switch-delay")
            .description("The delay in ticks to wait to break a crystal after switching hotbar slot.")
            .defaultValue(0)
            .min(0)
            .sliderMax(10)
            .build());

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
            .name("break-range")
            .description("Range in which to break crystals.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build());

    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder()
            .name("break-walls-range")
            .description("Range in which to break crystals when behind blocks.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build());

    private final Setting<Boolean> onlyBreakOwn = sgBreak.add(new BoolSetting.Builder()
            .name("only-own")
            .description("Only breaks own crystals.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> attemptCheck = sgBreak.add(new BoolSetting.Builder()
            .name("break-attempt-check")
            .description("To pair break attempt with damage calc or not.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> breakAttempts = sgBreak.add(new IntSetting.Builder()
            .name("break-attempts")
            .description("How many times to hit a crystal before stopping to target it.")
            .defaultValue(2)
            .sliderRange(0,5)
            .build());

    private final Setting<Boolean> ageCheck = sgBreak.add(new BoolSetting.Builder()
            .name("age-check")
            .description("To check crystal age or not.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> ticksExisted = sgBreak.add(new IntSetting.Builder()
            .name("ticks-existed")
            .description("Amount of ticks a crystal needs to have lived for it to be attacked by CrystalAura.")
            .defaultValue(1)
            .min(1)
            .visible(ageCheck::get)
            .build());

    private final Setting<Integer> attackFrequency = sgBreak.add(new IntSetting.Builder()
            .name("attack-frequency")
            .description("Maximum hits to do per second.")
            .defaultValue(25)
            .min(1)
            .sliderMax(30)
            .build());

    private final Setting<Boolean> antiWeakness = sgBreak.add(new BoolSetting.Builder()
            .name("anti-weakness")
            .description("Switches to tools with high enough damage to explode the crystal with weakness effect.")
            .defaultValue(true)
            .build());

    // Fast break

    private final Setting<Boolean> fastBreak = sgFastBreak.add(new BoolSetting.Builder()
            .name("fast-break")
            .description("Ignores break delay and tries to break the crystal as soon as it's spawned in the world.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> freqCheck = sgFastBreak.add(new BoolSetting.Builder()
            .name("attack-frequency-check")
            .description("Will not try to fast break if your attack exceeds the attack frequency.")
            .defaultValue(true)
            .visible(fastBreak::get)
            .build());

    private final Setting<Boolean> smartCheck = sgFastBreak.add(new BoolSetting.Builder()
            .name("smart-check")
            .description("Will not try to fast break for slow face place / surround hold.")
            .defaultValue(true)
            .visible(fastBreak::get)
            .build());

    private final Setting<Boolean> minDcheck = sgFastBreak.add(new BoolSetting.Builder()
            .name("min-damage-check")
            .description("Check if the crystal meets min damage first.")
            .defaultValue(true)
            .visible(fastBreak::get)
            .build());

    // Chain Pop

    public final Setting<Boolean> selfPopInvincibility = sgChainPop.add(new BoolSetting.Builder()
            .name("self-pop-invincibility")
            .description("Ignores self damage if you just popped.")
            .defaultValue(false)
            .build());

    public final Setting<Integer> selfPopInvincibilityTime = sgChainPop.add(new IntSetting.Builder()
            .name("self-pop-time")
            .description("How many millisecond to consider for self-pop invincibility")
            .defaultValue(300)
            .sliderRange(1,2000)
            .visible(selfPopInvincibility::get)
            .build());

    public final Setting<SelfPopIgnore> selfPopIgnore = sgChainPop.add(new EnumSetting.Builder<SelfPopIgnore>()
            .name("self-pop-ignore")
            .description("What to ignore when you just popped.")
            .defaultValue(SelfPopIgnore.Break)
            .visible(selfPopInvincibility::get)
            .build());

    public final Setting<Boolean> targetPopInvincibility = sgChainPop.add(new BoolSetting.Builder()
            .name("target-pop-invincibility")
            .description("Tries to pause certain actions when your enemy just popped.")
            .defaultValue(false)
            .build());

    public final Setting<Integer> targetPopInvincibilityTime = sgChainPop.add(new IntSetting.Builder()
            .name("target-pop-time")
            .description("How many milliseconds to consider for target-pop invincibility")
            .defaultValue(500)
            .sliderRange(1,2000)
            .visible(targetPopInvincibility::get)
            .build());

    public final Setting<PopPause> popPause = sgChainPop.add(new EnumSetting.Builder<PopPause>()
            .name("pop-pause-mode")
            .description("What to pause when your enemy just popped.")
            .defaultValue(PopPause.Break)
            .visible(targetPopInvincibility::get)
            .build());

    // Pause

    private final Setting<Double> pauseAtHealth = sgPause.add(new DoubleSetting.Builder()
            .name("pause-health")
            .description("Pauses when you go below a certain health.")
            .defaultValue(5)
            .min(0)
            .build());

    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses Crystal Aura when eating.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses Crystal Aura when drinking.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Pauses Crystal Aura when mining.")
            .defaultValue(false)
            .build());


    // Render
    private final Setting<RenderMode> mode = sgRender.add(new EnumSetting.Builder<RenderMode>()
            .name("render-mode")
            .description("The mode to render in.")
            .defaultValue(RenderMode.Normal)
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255, 45))
            .visible(() -> mode.get() != RenderMode.None && shapeMode.get() != ShapeMode.Lines)
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> mode.get() != RenderMode.None && shapeMode.get() != ShapeMode.Sides)
            .build());

    private final Setting<Boolean> renderDamageText = sgRender.add(new BoolSetting.Builder()
            .name("damage")
            .description("Renders crystal damage text in the block overlay.")
            .defaultValue(true)
            .visible(() -> mode.get() != RenderMode.None)
            .build());

    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder()
            .name("damage-scale")
            .description("How big the damage text should be.")
            .defaultValue(1.25)
            .min(1)
            .sliderMax(4)
            .visible(() -> mode.get() != RenderMode.None && renderDamageText.get())
            .build());

    private final Setting<SettingColor> damageTextColor = sgRender.add(new ColorSetting.Builder()
            .name("damage-color")
            .description("What the color of the damage text should be.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> mode.get() != RenderMode.None && renderDamageText.get())
            .build());

    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder()
            .name("place-time")
            .description("How long to render for.")
            .defaultValue(10)
            .min(0)
            .sliderMax(20)
            .visible(() -> mode.get() == RenderMode.Normal)
            .build());

    private final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder()
            .name("break")
            .description("Renders a block overlay over the block the crystals are broken on.")
            .defaultValue(false)
            .build());

    private final Setting<SettingColor> sideColorB = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255, 45))
            .visible(() -> renderBreak.get() && shapeMode.get() != ShapeMode.Lines)
            .build());

    private final Setting<SettingColor> lineColorB = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> renderBreak.get() && shapeMode.get() != ShapeMode.Sides)
            .build());

    private final Setting<Integer> renderBreakTime = sgRender.add(new IntSetting.Builder()
            .name("break-time")
            .description("How long to render breaking for.")
            .defaultValue(13)
            .min(0)
            .sliderMax(20)
            .visible(()-> renderBreak.get() && mode.get() == RenderMode.Normal)
            .build());

    private final Setting<Integer> fadeTime = sgRender.add(new IntSetting.Builder()
            .name("fade-time")
            .description("Tick duration for rendering placing.")
            .defaultValue(8)
            .range(0, 40)
            .sliderRange(0, 40)
            .visible(()-> (renderBreak.get() ||  mode.get() == RenderMode.Fade))
            .build());

    private final Setting<Integer> fadeAmount = sgRender.add(new IntSetting.Builder()
            .name("fade-amount")
            .description("How strong the fade should be.")
            .defaultValue(8)
            .range(0, 100)
            .sliderRange(0, 100)
            .visible(()-> (renderBreak.get() || mode.get() == RenderMode.Fade))
            .build());

    // Fields

    public int breakTimer;
    private int placeTimer;
    private int switchTimer;
    private int ticksPassed;
    public final List<PlayerEntity> targets = new ArrayList<>();

    private final Vec3d vec3d = new Vec3d(0, 0, 0);
    private final Vec3d playerEyePos = new Vec3d(0, 0, 0);
    private final Vec3 vec3 = new Vec3();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private final Box box = new Box(0, 0, 0, 0, 0, 0);

    private final Vec3d vec3dRayTraceEnd = new Vec3d(0, 0, 0);
    private RaycastContext raycastContext;

    private final IntSet placedCrystals = new IntOpenHashSet();
    private boolean placing;
    private int placingTimer;
    public final BlockPos.Mutable placingCrystalBlockPos = new BlockPos.Mutable();

    private final IntSet removed = new IntOpenHashSet();
    private final Int2IntMap attemptedBreaks = new Int2IntOpenHashMap();
    private final Int2IntMap waitingToExplode = new Int2IntOpenHashMap();
    public int attacks;

    private double serverYaw;

    public PlayerEntity bestTarget;
    private float bestTargetDamage;
    private int bestTargetTimer;

    private boolean didRotateThisTick;
    private boolean isLastRotationPos;
    private final Vec3d lastRotationPos = new Vec3d(0, 0 ,0);
    private double lastYaw, lastPitch;
    private int lastRotationTimer;

    public Timer selfPoppedTimer = new Timer();
    public Timer targetPoppedTimer = new Timer();

    private int renderTimer, breakRenderTimer;
    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();
    private final BlockPos.Mutable breakRenderPos = new BlockPos.Mutable();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private final Pool<RenderBlock> renderBreakBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBreakBlocks = new ArrayList<>();

    private double renderDamage;

    public BananaBomber() {
        super(AddModule.COMBAT, "banana-bomber", "Automatically places and attacks crystals.");
    }

    @Override
    public void onActivate() {
        breakTimer = 0;
        placeTimer = 0;
        ticksPassed = 0;

        raycastContext = new RaycastContext(new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        placing = false;
        placingTimer = 0;

        attacks = 0;

        serverYaw = mc.player.getYaw();

        bestTargetDamage = 0;
        bestTargetTimer = 0;

        lastRotationTimer = getLastRotationStopDelay();

        renderTimer = 0;
        breakRenderTimer = 0;

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();

        for (RenderBlock renderBlock : renderBreakBlocks) renderBreakBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        targets.clear();

        placedCrystals.clear();

        attemptedBreaks.clear();
        waitingToExplode.clear();

        removed.clear();

        bestTarget = null;

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();

        for (RenderBlock renderBlock : renderBreakBlocks) renderBreakBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    private int getLastRotationStopDelay() {
        return Math.max(10, placeDelay.get() / 2 + breakDelay.get() / 2 + 10);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPreTick(TickEvent.Pre event) {
        // Update last rotation
        didRotateThisTick = false;
        lastRotationTimer++;

        // Decrement placing timer
        if (placing) {
            if (placingTimer > 0) placingTimer--;
            else placing = false;
        }

        if (ticksPassed < 20) ticksPassed++;
        else {
            ticksPassed = 0;
            attacks = 0;
        }

        // Decrement best target timer
        if (bestTargetTimer > 0) bestTargetTimer--;
        bestTargetDamage = 0;

        // Decrement break, place and switch timers
        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
        if (switchTimer > 0) switchTimer--;

        // Decrement render timers
        if (renderTimer > 0) renderTimer--;
        if (breakRenderTimer > 0) breakRenderTimer--;

        // Update waiting to explode crystals and mark them as existing if reached threshold
        for (IntIterator it = waitingToExplode.keySet().iterator(); it.hasNext();) {
            int id = it.nextInt();
            int ticks = waitingToExplode.get(id);

            if (ticks >= waiting.get()) {
                it.remove();
                removed.remove(id);
            }
            else {
                waitingToExplode.put(id, ticks + 1);
            }
        }

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
            doBreak();
            doPlace();
        }

        if ((cancelCrystalMode.get() == CancelCrystalMode.Hit)) {
            removed.forEach((java.util.function.IntConsumer) id -> Objects.requireNonNull(mc.world.getEntityById(id)).kill());
            removed.clear();
        }

        // Ticking fade animation
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        renderBreakBlocks.forEach(RenderBlock::tick);
        renderBreakBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
    }

    @EventHandler(priority = EventPriority.LOWEST - 666)
    private void onPreTickLast(TickEvent.Pre event) {
        // Rotate to last rotation
        if (rotate.get() && lastRotationTimer < getLastRotationStopDelay() && !didRotateThisTick) {
            Rotations.rotate(isLastRotationPos ? Rotations.getYaw(lastRotationPos) : lastYaw, isLastRotationPos ? Rotations.getPitch(lastRotationPos) : lastPitch, -100, null);
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        if (placing && event.entity.getBlockPos().equals(placingCrystalBlockPos)) {
            placing = false;
            placingTimer = 0;
            placedCrystals.add(event.entity.getId());
        }

        if (fastBreak.get()) {
            if (freqCheck.get()) {
                if (attacks > attackFrequency.get()) return;
            }

            if (smartCheck.get()) {
                if (CrystalUtil.isSurroundHolding() || (slowFacePlace.get() && CrystalUtil.isFacePlacing() || (targetPopInvincibility.get() && CrystalUtil.targetJustPopped()))) return;
            }

            float damage = getBreakDamage(event.entity, false);
            if (minDcheck.get()) {
                if (damage < BminDamage.get()) return;
            }

            doBreak(event.entity);
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity instanceof EndCrystalEntity) {
            placedCrystals.remove(event.entity.getId());
            removed.remove(event.entity.getId());
            waitingToExplode.remove(event.entity.getId());
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
        if (!doBreak.get() || breakTimer > 0 || switchTimer > 0 || attacks >= attackFrequency.get() || (popPause.get() != PopPause.Place && CrystalUtil.targetJustPopped())) return;

        float bestDamage = 0;
        Entity crystal = null;

        // Find best crystal to break
        for (Entity entity : mc.world.getEntities()) {
            float damage = getBreakDamage(entity, true);

            if (damage > bestDamage) {
                bestDamage = damage;
                crystal = entity;
            }
        }

        // Break the crystal
        if (crystal != null) doBreak(crystal);
    }

    private float getBreakDamage(Entity entity, boolean checkCrystalAge) {
        if (!(entity instanceof EndCrystalEntity)) return 0;

        // Check only break own
        if (onlyBreakOwn.get() && !placedCrystals.contains(entity.getId())) return 0;

        // Check if it should already be removed
        if (removed.contains(entity.getId())) return 0;

        // Check attempted breaks
        if (attemptCheck.get()) {
            if (attemptedBreaks.get(entity.getId()) > breakAttempts.get()) return 0;
        }

        // Check crystal age
        if (ageCheck.get()) {
            if (checkCrystalAge && entity.age < ticksExisted.get()) return 0;
        }

        if (CrystalUtil.isSurroundHolding() && surroundHoldMode.get() != SlowMode.Delay) {
            if (checkCrystalAge && entity.age < surroundHoldAge.get()) return 0;
        }

        if (slowFacePlace.get() && slowFPMode.get() != SlowMode.Delay && CrystalUtil.isFacePlacing() && bestTarget != null && bestTarget.getY() < placingCrystalBlockPos.getY()) {
            if (checkCrystalAge && entity.age < slowFPAge.get()) return 0;
        }

        // Check range
        if (isOutOfBreakRange(entity)) return 0;

        // Check damage to self and anti suicide
        blockPos.set(entity.getBlockPos()).move(0, -1, 0);

        if (!CrystalUtil.shouldIgnoreSelfBreakDamage()) {
            float selfDamage = BPlusDamageUtils.crystalDamage(mc.player, entity.getPos(), predictMovement.get(), selfBreakExplosionRadius.get().floatValue(), ignoreTerrain.get(), fullAnvil.get(), fullEchest.get());
            if (selfDamage > BmaxDamage.get() || (BantiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)))
                return 0;
        } else if (debug.get()) warning("Ignoring self break dmg");

        // Check damage to targets and face place
        float damage = getDamageToTargets(entity.getPos(), true, false);
        boolean facePlaced = (facePlace.get() && CrystalUtil.shouldFacePlace(blockPos) || forceFacePlace.get().isPressed());

        if (!facePlaced && damage < BminDamage.get()) return 0;

        return damage;
    }

    private void doBreak(Entity crystal) {
        // Anti weakness
        if (antiWeakness.get()) {
            StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
            StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);

            // Check for strength
            if (weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier())) {
                // Check if the item in your hand is already valid
                if (!isValidWeaknessItem(mc.player.getMainHandStack())) {
                    // Find valid item to break with
                    if (!InvUtils.swap(InvUtils.findInHotbar(this::isValidWeaknessItem).slot(), false)) return;

                    switchTimer = 1;
                    return;
                }
            }
        }

        // Rotate and attack
        boolean attacked = true;

        if (!rotate.get()) {
            CrystalUtil.attackCrystal(crystal);
        }
        else {
            double yaw = Rotations.getYaw(crystal);
            double pitch = Rotations.getPitch(crystal, Target.Feet);

            if (doYawSteps(yaw, pitch)) {
                setRotation(true, crystal.getPos(), 0, 0);
                Rotations.rotate(yaw, pitch, 50, () -> CrystalUtil.attackCrystal(crystal));
            }
            else {
                attacked = false;
            }
        }

        if (attacked) {
            // Update state
            removed.add(crystal.getId());
            attemptedBreaks.put(crystal.getId(), attemptedBreaks.get(crystal.getId()) + 1);
            waitingToExplode.put(crystal.getId(), 0);

            // Break render
            renderBreakBlocks.add(renderBreakBlockPool.get().set(crystal.getBlockPos().down()));
            breakRenderPos.set(crystal.getBlockPos().down());
            breakRenderTimer = renderBreakTime.get();
        }
    }

    private boolean isValidWeaknessItem(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ToolItem) || itemStack.getItem() instanceof HoeItem) return false;

        ToolMaterial material = ((ToolItem) itemStack.getItem()).getMaterial();
        return material == ToolMaterials.DIAMOND || material == ToolMaterials.NETHERITE;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer = switchDelay.get();
        }
    }

    // Place

    private void doPlace() {
        if (!doPlace.get() || placeTimer > 0 || (popPause.get() != PopPause.Break && CrystalUtil.targetJustPopped())) return;

        // Return if there are no crystals in hotbar or offhand
        if (!InvUtils.findInHotbar(Items.END_CRYSTAL).found()) return;

        // Return if there are no crystals in either hand and auto switch mode is none
        if (autoSwitch.get() == AutoSwitchMode.None && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) return;

        // Check for multiplace
        for (Entity entity : mc.world.getEntities()) {
            if (getBreakDamage(entity, false) > 0) return;
        }

        // Setup variables
        AtomicDouble bestDamage = new AtomicDouble(0);
        AtomicReference<BlockPos.Mutable> bestBlockPos = new AtomicReference<>(new BlockPos.Mutable());
        AtomicBoolean isSupport = new AtomicBoolean(support.get() != SupportMode.Disabled);

        // Find best position to place the crystal on
        BlockIterator.register((int) Math.ceil(placeRange.get()), (int) Math.ceil(placeVerticalRange.get()), (bp, blockState) -> {
            // Todo : implement a method to check more efficiently instead of using the blockstate check, maybe another check can come in before them to see if they're a solid block or not since it takes too much resources (maybe a fullcube check?)

            // Check if its bedrock or obsidian and return if isSupport is false
            boolean hasBlock = blockState.isOf(Blocks.BEDROCK) || blockState.isOf(Blocks.OBSIDIAN);

            // Check for support
            if (!hasBlock) {
                if (isSupport.get()) {
                    if (!blockState.getMaterial().isReplaceable()) return;
                } else return;
            }

            // Check if there is air on top
            blockPos.set(bp.getX(), bp.getY() + 1, bp.getZ());
            if (!mc.world.getBlockState(blockPos).isAir()) return;

            if (placement112.get()) {
                blockPos.move(0, 1, 0);
                if (!mc.world.getBlockState(blockPos).isAir()) return;
            }

            // Check range
            ((IVec3d) vec3d).set(bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 0.5);
            blockPos.set(bp).move(0, 1, 0);
            if (isOutOfPlaceRange(vec3d, blockPos)) return;

            // Check if it can be placed
            int x = bp.getX();
            int y = bp.getY() + 1;
            int z = bp.getZ();
            // Weird bug this is prolly a temporary fix
            ((IBox) box).set(x + 0.001, y, z + 0.001, x + 0.999, y + (smallBox.get() ? 1 : 2), z + 0.999);

            if (intersectsWithEntities(box)) return;

            // Check damage to self and anti suicide
            if (!CrystalUtil.shouldIgnoreSelfPlaceDamage()) {
                float selfDamage = BPlusDamageUtils.crystalDamage(mc.player, vec3d, predictMovement.get(), selfPlaceExplosionRadius.get().floatValue(), ignoreTerrain.get(), fullAnvil.get(), fullEchest.get());
                if (selfDamage > PmaxDamage.get() || (PantiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)))
                    return;
            } else if (debug.get()) warning("Ignoring self place dmg");

            // Check damage to targets and face place
            float damage = getDamageToTargets(vec3d, false, !hasBlock && support.get() == SupportMode.Fast);

            boolean facePlaced = (facePlace.get() && CrystalUtil.shouldFacePlace(blockPos) || forceFacePlace.get().isPressed());

            boolean burrowBreaking = (CrystalUtil.isBurrowBreaking() && CrystalUtil.shouldBurrowBreak(blockPos));

            boolean surroundBreaking = (CrystalUtil.isSurroundBreaking() && CrystalUtil.shouldSurroundBreak(blockPos));

            if ((!facePlaced && !surroundBreaking && !burrowBreaking) && damage < PminDamage.get()) return;

            // Compare damage
            if (damage > bestDamage.get() || (isSupport.get() && hasBlock)) {
                bestDamage.set(damage);
                bestBlockPos.get().set(bp);
            }

            if (hasBlock) isSupport.set(false);
        });

        // Place the crystal
        BlockIterator.after(() -> {
            if (bestDamage.get() == 0) return;

            BlockHitResult result = getPlaceInfo(bestBlockPos.get());

            ((IVec3d) vec3d).set(
                result.getBlockPos().getX() + 0.5 + result.getSide().getVector().getX() * 0.5,
                result.getBlockPos().getY() + 0.5 + result.getSide().getVector().getY() * 0.5,
                result.getBlockPos().getZ() + 0.5 + result.getSide().getVector().getZ() * 0.5
            );

            if (rotate.get()) {
                double yaw = Rotations.getYaw(vec3d);
                double pitch = Rotations.getPitch(vec3d);

                if (yawStepMode.get() == YawStepMode.Break || doYawSteps(yaw, pitch)) {
                    setRotation(true, vec3d, 0, 0);
                    Rotations.rotate(yaw, pitch, 50, () -> placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null));

                    placeTimer += CrystalUtil.getPlaceDelay();
                }
            }
            else {
                placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null);
                placeTimer += CrystalUtil.getPlaceDelay();
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

    private void placeCrystal(BlockHitResult result, double damage, BlockPos supportBlock) {
        // Switch
        Item targetItem = supportBlock == null ? Items.END_CRYSTAL : Items.OBSIDIAN;

        FindItemResult item = InvUtils.findInHotbar(targetItem);
        if (!item.found()) return;

        int prevSlot = mc.player.getInventory().selectedSlot;

        if (!(mc.player.getOffHandStack().getItem() instanceof EndCrystalItem) && (autoSwitch.get() == AutoSwitchMode.Normal && noGapSwitch.get()) && (mc.player.getMainHandStack().getItem() instanceof EnchantedGoldenAppleItem)) return;
        if (autoSwitch.get() != AutoSwitchMode.None && !item.isOffhand()) InvUtils.swap(item.slot(), false);

        Hand hand = item.getHand();
        if (hand == null) return;

        // Place
        if (supportBlock == null) {
            // Place crystal
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, result));

            if (placeSwing.get()) mc.player.swingHand(hand);
            if (!ghostPlace.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

            if (debug.get()) warning("Placing");

            placing = true;
            placingTimer = 4;
            placingCrystalBlockPos.set(result.getBlockPos()).move(0, 1, 0);


            //TODO: fix so this doesn't add if its already in the list

            renderBlocks.add(renderBlockPool.get().set(result.getBlockPos()));

            renderTimer = renderTime.get();
            renderPos.set(result.getBlockPos());
            renderDamage = damage;
        }
        else {
            // Place support block
            BlockUtils.place(supportBlock, item, false, 0, placeSwing.get(), true, false);
            placeTimer += supportDelay.get();

            if (supportDelay.get() == 0) placeCrystal(result, damage, null);
        }

        // Switch back
        if (autoSwitch.get() == AutoSwitchMode.Silent) InvUtils.swap(prevSlot, false);
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
        }
        else {
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

    private boolean isOutOfPlaceRange(Vec3d vec3d, BlockPos blockPos) {
        ((IRaycastContext) raycastContext).set(playerEyePos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        BlockHitResult result = mc.world.raycast(raycastContext);
        boolean behindWall = result == null || !result.getBlockPos().equals(blockPos);
        double distance = mc.player.getEyePos().distanceTo(vec3d);

        return distance > (behindWall ? placeWallsRange.get() : placeRange.get());
    }

    private boolean isOutOfBreakRange(Entity entity) {
        boolean behindWall = !mc.player.canSee(entity);
        double distance = BPlusPlayerUtils.distanceFromEye(entity);

        return distance > (behindWall ? breakWallsRange.get() : breakRange.get());
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
            if (!(smartDelay.get() && breaking && target.hurtTime > 0)) damage = BPlusDamageUtils.crystalDamage(target, vec3d, predictMovement.get(), explosionRadiusToTarget.get().floatValue(), ignoreTerrain.get(), fullAnvil.get(), fullEchest.get());
        }
        else {
            for (PlayerEntity target : targets) {
                if (smartDelay.get() && breaking && target.hurtTime > 0) continue;

                float dmg = BPlusDamageUtils.crystalDamage(target, vec3d, predictMovement.get(), explosionRadiusToTarget.get().floatValue(), ignoreTerrain.get(), fullAnvil.get(), fullEchest.get());

                // Update best target
                if (dmg > bestTargetDamage) {
                    bestTarget = target;
                    bestTargetDamage = dmg;
                    bestTargetTimer = 10;
                }

                // Todo : this part is broken as fuck, it should be = not += but = will fuck a lot of scenarios up when there's multiple targets so  just using this for now until i find a fix
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

    private boolean intersectsWithEntities(Box box) {
        return EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator() && !removed.contains(entity.getId()));
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket)) return;

        EntityStatusS2CPacket p = (EntityStatusS2CPacket) event.packet;
        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);

        if (!(entity instanceof PlayerEntity)) return;

        if (entity.equals(mc.player) && selfPopInvincibility.get()) selfPoppedTimer.reset();

        if (entity.equals(bestTarget) && targetPopInvincibility.get()) targetPoppedTimer.reset();

    }

    @EventHandler(priority = EventPriority.LOWEST - 1000)
    private void onTick(TickEvent.Post event) {
        if (debug.get()) {
            if (CrystalUtil.isFacePlacing() && bestTarget != null && bestTarget.getY() < placingCrystalBlockPos.getY()) {
                if (slowFacePlace.get()) warning("Slow faceplacing");
                else warning("Faceplacing");
            }

            if (CrystalUtil.isBurrowBreaking()) warning("Burrow breaking");

            if (CrystalUtil.isSurroundHolding()) warning("Surround holding");

            if (CrystalUtil.isSurroundBreaking()) warning("Surround breaking");
        }
    }

    // Render
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mode.get() == RenderMode.Fade) {
            renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
            renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));

            if (renderBreak.get()) {
                renderBreakBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                renderBreakBlocks.forEach(renderBlock -> renderBlock.render(event, sideColorB.get(), lineColorB.get(), shapeMode.get()));
            }
        } else if (mode.get() == RenderMode.Normal) {
            if (renderTimer > 0) {
                event.renderer.box(renderPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }

            if (breakRenderTimer > 0 && renderBreak.get() && !mc.world.getBlockState(breakRenderPos).isAir()) {
                int preSideA = sideColor.get().a;
                sideColor.get().a -= 20;
                sideColor.get().validate();

                int preLineA = lineColorB.get().a;
                lineColorB.get().a -= 20;
                lineColorB.get().validate();

                event.renderer.box(breakRenderPos, sideColorB.get(), lineColorB.get(), shapeMode.get(), 0);

                sideColorB.get().a = preSideA;
                lineColorB.get().a = preLineA;
            }
        }

    }

    public class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = fadeTime.get();

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

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mode.get() == RenderMode.None|| renderTimer <= 0 || !renderDamageText.get()) return;

        vec3.set(renderPos.getX() + 0.5, renderPos.getY() + 0.5, renderPos.getZ() + 0.5);

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

    public PlayerEntity getPlayerTarget() {
        if(bestTarget != null) {
            return bestTarget;
        } else {
            return null;
        }
    }
}
