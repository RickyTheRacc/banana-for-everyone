package me.ricky.banana.system;

import me.ricky.banana.BananaPlus;
import me.ricky.banana.enums.AntiCheat;
import me.ricky.banana.enums.SwitchMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BananaConfig extends System<BananaConfig> {
    public final Settings settings = new Settings();

    private final SettingGroup sgPrefix = settings.createGroup("Prefix");
    private final SettingGroup sgText = settings.createGroup("3D Text");
    private final SettingGroup sgServer = settings.createGroup("Server");
    private final SettingGroup sgBlocks = settings.createGroup("Blocks");
    private final SettingGroup sgEntities = settings.createGroup("Entities");
    
    // Prefix

    public final Setting<String> prefix = sgPrefix.add(new StringSetting.Builder()
        .name("banana+-prefix")
        .description("What prefix to use for Banana+ modules.")
        .defaultValue("Banana+")
        .onChanged(cope -> ChatUtils.registerCustomPrefix("me.ricky.banana", this::getPrefix))
        .build()
    );

    public final Setting<SettingColor> prefixColor = sgPrefix.add(new ColorSetting.Builder()
        .name("prefix-color")
        .description("Color display for the prefix.")
        .defaultValue(new SettingColor(255,193,0,255))
        .onChanged(cope -> ChatUtils.registerCustomPrefix("me.ricky.banana", this::getPrefix))
        .build()
    );

    public final Setting<Format> prefixFormat = sgPrefix.add(new EnumSetting.Builder<Format>()
        .name("prefix-format")
        .description("What type of minecraft formatting should be applied to the prefix.")
        .defaultValue(Format.Normal)
        .onChanged(cope -> ChatUtils.registerCustomPrefix("me.ricky.banana", this::getPrefix))
        .build()
    );

    public final Setting<Boolean> formatBrackets = sgPrefix.add(new BoolSetting.Builder()
        .name("format-brackets")
        .description("Whether the formatting should apply to the brackets as well.")
        .visible(() -> prefixFormat.get() != Format.Normal)
        .onChanged(cope -> ChatUtils.registerCustomPrefix("me.ricky.banana", this::getPrefix))
        .defaultValue(true)
        .build()
    );

    public final Setting<String> leftBracket = sgPrefix.add(new StringSetting.Builder()
        .name("left-bracket")
        .description("What to be displayed as left bracket for the prefix.")
        .defaultValue("[")
        .onChanged(cope -> ChatUtils.registerCustomPrefix("me.ricky.banana", this::getPrefix))
        .build()
    );

    public final Setting<String> rightBracket = sgPrefix.add(new StringSetting.Builder()
        .name("right-bracket")
        .description("What to be displayed as right bracket for the prefix.")
        .defaultValue("]")
        .onChanged(cope -> ChatUtils.registerCustomPrefix("me.ricky.banana", this::getPrefix))
        .build()
    );

    public final Setting<SettingColor> leftColor = sgPrefix.add(new ColorSetting.Builder()
        .name("left-color")
        .description("Color display for the left bracket.")
        .defaultValue(new SettingColor(150,150,150,255))
        .onChanged(cope -> ChatUtils.registerCustomPrefix("me.ricky.banana", this::getPrefix))
        .build()
    );

    public final Setting<SettingColor> rightColor = sgPrefix.add(new ColorSetting.Builder()
        .name("right-color")
        .description("Color display for the right bracket.")
        .defaultValue(new SettingColor(150,150,150,255))
        .onChanged(cope -> ChatUtils.registerCustomPrefix("me.ricky.banana", this::getPrefix))
        .build()
    );

    // Text

    private final Setting<Double> textScale = sgText.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The base scale of the text.")
        .defaultValue(1)
        .range(1,5)
        .build()
    );

    private final Setting<Double> divisor = sgText.add(new DoubleSetting.Builder()
        .name("divisor")
        .description("How strongly distance should affect text size.")
        .defaultValue(6)
        .range(1,10)
        .build()
    );

    private final Setting<Double> minScale = sgText.add(new DoubleSetting.Builder()
        .name("min-scale")
        .description("The smallest text can get, regardless of distance.")
        .defaultValue(0.5)
        .range(0.1,5)
        .build()
    );

    private final Setting<Double> maxScale = sgText.add(new DoubleSetting.Builder()
        .name("max-scale")
        .description("The largest text can get, regardless of distance.")
        .defaultValue(1.7)
        .range(0.1,5)
        .build()
    );

    // Server

    public final Setting <AntiCheat> antiCheat = sgServer.add(new EnumSetting.Builder<AntiCheat>()
        .name("anticheat")
        .description("Which anticheat the server uses.")
        .defaultValue(AntiCheat.NoCheat)
        .build()
    );

    public final Setting<Integer> minimumAge = sgServer.add(new IntSetting.Builder()
        .name("crystal-age")
        .description("How many ticks a crystal must exist before attacking it.")
        .defaultValue(0)
        .range(0,10)
        .sliderRange(0,10)
        .build()
    );

    public final Setting<Boolean> tpsSync = sgServer.add(new BoolSetting.Builder()
        .name("TPS-sync")
        .description("Attempt to sync certain modules' actions with the server TPS.")
        .defaultValue(true)
        .build()
    );

    // Blocks

    public final Setting<Double> blockRange = sgBlocks.add(new DoubleSetting.Builder()
        .name("block-range")
        .description("How far away you can interact with blocks.")
        .defaultValue(4.5)
        .range(0,6)
        .sliderRange(0,6)
        .build()
    );

    public final Setting<SwitchMode> switchMode = sgBlocks.add(new EnumSetting.Builder<SwitchMode>()
        .name("switch-mode")
        .description("How to switch to the blocks you want to place.")
        .defaultValue(SwitchMode.Silent)
        .build()
    );

    public final Setting<Integer> placeDelay = sgBlocks.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Tick delay between block placements.")
        .defaultValue(1)
        .range(0,20)
        .sliderRange(0,20)
        .build()
    );

    public final Setting<Integer> blocksPerTick = sgBlocks.add(new IntSetting.Builder()
        .name("blocks-/-tick")
        .description("How many blocks to place in one tick.")
        .defaultValue(3)
        .range(1,5)
        .sliderRange(1,5)
        .build()
    );

    public final Setting<Boolean> airPlace = sgBlocks.add(new BoolSetting.Builder()
        .name("airplace")
        .description("Try and place blocks in mid-air.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> blockRaytrace = sgBlocks.add(new BoolSetting.Builder()
        .name("raytrace")
        .description("Require line of sight to blocks you interact with.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> blockRotate = sgBlocks.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate towards blocks you're interacting with.")
        .defaultValue(false)
        .build()
    );

    // Entities

    public final Setting<Double> entityRange = sgEntities.add(new DoubleSetting.Builder()
        .name("entity-range")
        .description("How far away you can interact with entities.")
        .defaultValue(4.5)
        .range(0,6)
        .sliderRange(0,6)
        .build()
    );

    public final Setting<Integer> attacksPerSecond = sgEntities.add(new IntSetting.Builder()
        .name("attacks-/-second")
        .description("How many times you can attack entities per second.")
        .defaultValue(20)
        .range(1,20)
        .sliderRange(1,20)
        .build()
    );

    public final Setting<Boolean> hitboxes = sgEntities.add(new BoolSetting.Builder()
        .name("hitboxes")
        .description("Attack entities at the closest point in their hitbox instead of the center.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> entityRaytrace = sgEntities.add(new BoolSetting.Builder()
        .name("raytrace")
        .description("Only interact with entities you can see.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> entityRotate = sgEntities.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate towards entities you're interacting with.")
        .defaultValue(false)
        .build()
    );

    public BananaConfig() {
        super("banana+");
        ChatUtils.registerCustomPrefix("me.ricky.banana", this::getPrefix);
    }

    public static BananaConfig get() {
        return Systems.get(BananaConfig.class);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("version", BananaPlus.VERSION.toString());
        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public BananaConfig fromTag(NbtCompound tag) {
        if (tag.contains("settings")) settings.fromTag(tag.getCompound("settings"));
        return this;
    }

    public Text getPrefix() {
        MutableText logo = Text.literal(prefix.get());
        MutableText left = Text.literal(leftBracket.get());
        MutableText right = Text.literal(rightBracket.get());
        MutableText prefix = Text.literal("");

        if (prefixFormat.get() != Format.Normal) logo.setStyle(Style.EMPTY.withFormatting(prefixFormat.get().formatting));
        logo.setStyle(logo.getStyle().withColor(TextColor.fromRgb(prefixColor.get().getPacked())));

        if (prefixFormat.get() != Format.Normal && formatBrackets.get()) {
            left.setStyle(Style.EMPTY.withFormatting(prefixFormat.get().formatting));
            right.setStyle(Style.EMPTY.withFormatting(prefixFormat.get().formatting));
        }

        left.setStyle(left.getStyle().withColor(TextColor.fromRgb(leftColor.get().getPacked())));
        right.setStyle(right.getStyle().withColor(TextColor.fromRgb(rightColor.get().getPacked())));

        prefix.append(left);
        prefix.append(logo);
        prefix.append(right);
        prefix.append(" ");

        return prefix;
    }

    public enum Format {
        Normal(null),
        Heavy(Formatting.BOLD),
        Italic(Formatting.ITALIC),
        Underline(Formatting.UNDERLINE),
        Crossed(Formatting.STRIKETHROUGH),
        Cursed(Formatting.OBFUSCATED);

        final Formatting formatting;
        Format (Formatting formatting) {
            this.formatting = formatting;
        }
    }

    public double getScale(Vector3d pos) {
        double denom = pos.distance(
            mc.gameRenderer.getCamera().getPos().x,
            mc.gameRenderer.getCamera().getPos().y,
            mc.gameRenderer.getCamera().getPos().z
        ) / divisor.get();
        return MathHelper.clamp(textScale.get() / denom, minScale.get(), maxScale.get());
    }
}
