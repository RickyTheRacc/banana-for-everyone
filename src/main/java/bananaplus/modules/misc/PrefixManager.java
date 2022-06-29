package bananaplus.modules.misc;

import bananaplus.modules.BananaPlus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public class PrefixManager extends Module {
    public enum PrefixMode {
        Banana,
        Custom,
        Default
    }

    public enum Format {
        Normal,
        Heavy,
        Italic,
        Underline,
        Crossed,
        Cursed
    }


    private final SettingGroup sgBanana = settings.createGroup("Banana+");
    private final SettingGroup sgMeteor = settings.createGroup("Meteor");


    // General
    private final Setting<String> bananaPrefix = sgBanana.add(new StringSetting.Builder()
            .name("banana+-prefix")
            .description("What prefix to use for Banana+ modules.")
            .defaultValue("Banana+")
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<SettingColor> bananaColor = sgBanana.add(new ColorSetting.Builder()
            .name("prefix-color")
            .description("Color display for the prefix.")
            .defaultValue(new SettingColor(255,193,0,255))
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<Format> bananaFormatting = sgBanana.add(new EnumSetting.Builder<Format>()
            .name("prefix-format")
            .description("What type of minecraft formatting should be applied to the prefix.")
            .defaultValue(Format.Normal)
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<Boolean> bananaFormatBrackets = sgBanana.add(new BoolSetting.Builder()
            .name("format-brackets")
            .description("Whether the formatting should apply to the brackets as well.")
            .visible(() -> bananaFormatting.get() != Format.Normal)
            .onChanged(cope -> setPrefixes())
            .defaultValue(true)
            .build()
    );

    private final Setting<String> bananaLeftBracket = sgBanana.add(new StringSetting.Builder()
            .name("left-bracket")
            .description("What to be displayed as left bracket for the prefix.")
            .defaultValue("[")
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<String> bananaRightBracket = sgBanana.add(new StringSetting.Builder()
            .name("right-bracket")
            .description("What to be displayed as right bracket for the prefix.")
            .defaultValue("]")
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<SettingColor> bananaLeftColor = sgBanana.add(new ColorSetting.Builder()
            .name("left-color")
            .description("Color display for the left bracket.")
            .defaultValue(new SettingColor(150,150,150,255))
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<SettingColor> bananaRightColor = sgBanana.add(new ColorSetting.Builder()
            .name("right-color")
            .description("Color display for the right bracket.")
            .defaultValue(new SettingColor(150,150,150,255))
            .onChanged(cope -> setPrefixes())
            .build()
    );


    // Meteor
    private final Setting<PrefixMode> prefixMode = sgMeteor.add(new EnumSetting.Builder<PrefixMode>()
            .name("prefix-mode")
            .description("What prefix to use for Meteor modules.")
            .defaultValue(PrefixMode.Default)
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<String> meteorPrefix = sgMeteor.add(new StringSetting.Builder()
            .name("meteor-prefix")
            .description("What to use as meteor prefix text")
            .defaultValue("Motor")
            .visible(()-> prefixMode.get() == PrefixMode.Custom)
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<SettingColor> meteorColor = sgMeteor.add(new ColorSetting.Builder()
            .name("prefix-color")
            .description("Color display for the meteor prefix")
            .defaultValue(new SettingColor(142, 60, 222, 255))
            .visible(()-> prefixMode.get() == PrefixMode.Custom)
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<Format> meteorFormatting = sgMeteor.add(new EnumSetting.Builder<Format>()
            .name("prefix-format")
            .description("What type of minecraft formatting should be applied to the prefix.")
            .defaultValue(Format.Normal)
            .visible(()-> prefixMode.get() == PrefixMode.Custom)
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<Boolean> meteorFormatBrackets = sgBanana.add(new BoolSetting.Builder()
            .name("format-brackets")
            .description("Whether the formatting should apply to the brackets as well.")
            .visible(() -> prefixMode.get() == PrefixMode.Custom && meteorFormatting.get() != Format.Normal)
            .onChanged(cope -> setPrefixes())
            .defaultValue(true)
            .build()
    );

    private final Setting<String> meteorLeftBracket = sgMeteor.add(new StringSetting.Builder()
            .name("left-bracket")
            .description("What to be displayed as left bracket for the meteor prefix")
            .defaultValue("[")
            .visible(()-> prefixMode.get() == PrefixMode.Custom)
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<String> meteorRightBracket = sgMeteor.add(new StringSetting.Builder()
            .name("right-bracket")
            .description("What to be displayed as right bracket for the meteor prefix")
            .defaultValue("]")
            .visible(()-> prefixMode.get() == PrefixMode.Custom)
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<SettingColor> meteorLeftColor = sgMeteor.add(new ColorSetting.Builder()
            .name("left-color")
            .description("Color display for the left bracket")
            .defaultValue(new SettingColor(150,150,150,255))
            .visible(()-> prefixMode.get() == PrefixMode.Custom)
            .onChanged(cope -> setPrefixes())
            .build()
    );

    private final Setting<SettingColor> meteorRightColor = sgMeteor.add(new ColorSetting.Builder()
            .name("right-color")
            .description("Color display for the right bracket")
            .defaultValue(new SettingColor(150,150,150,255))
            .visible(()-> prefixMode.get() == PrefixMode.Custom)
            .onChanged(cope -> setPrefixes())
            .build()
    );


    public PrefixManager() {
        super(BananaPlus.MISC, "prefix-manager", "Allows you to customize prefixes used by Meteor.");
    }


    @Override
    public void onActivate(){
        setPrefixes();
    }

    @Override
    public void onDeactivate() {
        ChatUtils.unregisterCustomPrefix("bananaplus.modules");
        ChatUtils.unregisterCustomPrefix("meteordevelopment");
    }

    public void setPrefixes() {
        if (isActive()) {
            ChatUtils.registerCustomPrefix("bananaplus.modules", this::getBananaPrefix);

            switch (prefixMode.get()) {
                case Banana -> ChatUtils.registerCustomPrefix("meteordevelopment", this::getBananaPrefix);
                case Custom -> ChatUtils.registerCustomPrefix("meteordevelopment", this::getMeteorPrefix);
                case Default -> ChatUtils.unregisterCustomPrefix("meteordevelopment");
            }
        }
    }

    private Formatting getFormat(Format format) {
        return switch (format) {
            case Normal -> null;
            case Heavy -> Formatting.BOLD;
            case Italic -> Formatting.ITALIC;
            case Underline -> Formatting.UNDERLINE;
            case Cursed -> Formatting.OBFUSCATED;
            case Crossed -> Formatting.STRIKETHROUGH;
        };
    }

    public Text getBananaPrefix() {
        MutableText logo = Text.literal(bananaPrefix.get());
        MutableText left = Text.literal(bananaLeftBracket.get());
        MutableText right = Text.literal(bananaRightBracket.get());
        MutableText prefix = Text.literal("");

        if (bananaFormatting.get() != Format.Normal) logo.setStyle(Style.EMPTY.withFormatting(getFormat(bananaFormatting.get())));
        logo.setStyle(logo.getStyle().withColor(TextColor.fromRgb(bananaColor.get().getPacked())));

        if (bananaFormatting.get() != Format.Normal && bananaFormatBrackets.get()) left.setStyle(Style.EMPTY.withFormatting(getFormat(bananaFormatting.get())));
        if (bananaFormatting.get() != Format.Normal && bananaFormatBrackets.get()) right.setStyle(Style.EMPTY.withFormatting(getFormat(bananaFormatting.get())));
        left.setStyle(left.getStyle().withColor(TextColor.fromRgb(bananaLeftColor.get().getPacked())));
        right.setStyle(right.getStyle().withColor(TextColor.fromRgb(bananaRightColor.get().getPacked())));

        prefix.append(left);
        prefix.append(logo);
        prefix.append(right);
        prefix.append(" ");

        return prefix;
    }

    public Text getMeteorPrefix() {
        MutableText logo = Text.literal(meteorPrefix.get());
        MutableText left = Text.literal(meteorLeftBracket.get());
        MutableText right = Text.literal(meteorRightBracket.get());
        MutableText prefix = Text.literal("");

        if (meteorFormatting.get() != Format.Normal) logo.setStyle(Style.EMPTY.withFormatting(getFormat(meteorFormatting.get())));
        logo.setStyle(logo.getStyle().withColor(TextColor.fromRgb(meteorColor.get().getPacked())));

        if (meteorFormatting.get() != Format.Normal && meteorFormatBrackets.get()) left.setStyle(Style.EMPTY.withFormatting(getFormat(bananaFormatting.get())));
        if (meteorFormatting.get() != Format.Normal && meteorFormatBrackets.get()) right.setStyle(Style.EMPTY.withFormatting(getFormat(bananaFormatting.get())));
        left.setStyle(left.getStyle().withColor(TextColor.fromRgb(meteorLeftColor.get().getPacked())));
        right.setStyle(right.getStyle().withColor(TextColor.fromRgb(meteorRightColor.get().getPacked())));

        prefix.append(left);
        prefix.append(logo);
        prefix.append(right);
        prefix.append(" ");

        return prefix;
    }
}
