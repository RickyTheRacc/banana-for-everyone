package bananaplus.modules.misc;

import bananaplus.modules.BananaPlus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.MutableRegistry;

public class BPrefix extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General
    private final Setting<String> prefix = sgGeneral.add(new StringSetting.Builder()
            .name("prefix")
            .description("What to be displayed as Banana+ Prefix.")
            .defaultValue("Banana+")
            .build()
    );

    private final Setting<SettingColor> prefixColors = sgGeneral.add(new ColorSetting.Builder()
            .name("prefix-color")
            .description("Color display for the prefix.")
            .defaultValue(new SettingColor(255,193,0,255))
            .build()
    );

    private final Setting<String> leftBracket = sgGeneral.add(new StringSetting.Builder()
            .name("left-bracket")
            .description("What to be displayed as left bracket for the prefix.")
            .defaultValue("[")
            .build()
    );

    private final Setting<String> rightBracket = sgGeneral.add(new StringSetting.Builder()
            .name("right-bracket")
            .description("What to be displayed as right bracket for the prefix.")
            .defaultValue("]")
            .build()
    );

    private final Setting<SettingColor> leftBracketColor = sgGeneral.add(new ColorSetting.Builder()
            .name("left-bracket-color")
            .description("Color display for the left bracket.")
            .defaultValue(new SettingColor(150,150,150,255))
            .build()
    );

    private final Setting<SettingColor> rightBracketColor = sgGeneral.add(new ColorSetting.Builder()
            .name("right-bracket-color")
            .description("Color display for the right bracket.")
            .defaultValue(new SettingColor(150,150,150,255))
            .build()
    );

    private final Setting<Boolean> override = sgGeneral.add(new BoolSetting.Builder()
            .name("override")
            .description("Overrides the Meteor prefix with the b+ one.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> mprefix = sgGeneral.add(new BoolSetting.Builder()
            .name("Meteor Prefix")
            .description("Custom prefix for meteor modules")
            .defaultValue(false)
            .visible(()-> !override.get())
            .build());

    private final Setting<String> mprefixname = sgGeneral.add(new StringSetting.Builder()
            .name("Prefix")
            .description("What to use as meteor prefix text")
            .defaultValue("Motor")
            .visible(()-> !override.get() && mprefix.get())
            .build());

    private final Setting<SettingColor> mprefixColors = sgGeneral.add(new ColorSetting.Builder()
            .name("Prefix Color")
            .description("Color display for the meteor prefix")
            .defaultValue(new SettingColor(170, 0, 255, 100))
            .visible(()-> !override.get() && mprefix.get())
            .build());

    private final Setting<String> mleftBracket = sgGeneral.add(new StringSetting.Builder()
            .name("Left Bracket")
            .description("What to be displayed as left bracket for the meteor prefix")
            .defaultValue("[")
            .visible(()-> !override.get() && mprefix.get())
            .build());

    private final Setting<String> mrightBracket = sgGeneral.add(new StringSetting.Builder()
            .name("Right Bracket")
            .description("What to be displayed as right bracket for the meteor prefix")
            .defaultValue("]")
            .visible(()-> !override.get() && mprefix.get())
            .build());

    private final Setting<SettingColor> motorLeftBracketColor = sgGeneral.add(new ColorSetting.Builder()
            .name("Left Bracket Color")
            .description("Color display for the left bracket")
            .defaultValue(new SettingColor(128, 128, 128, 128))
            .visible(()-> !override.get() && mprefix.get())
            .build());

    private final Setting<SettingColor> motorRightBracketColor = sgGeneral.add(new ColorSetting.Builder()
            .name("Right Bracket Color")
            .description("Color display for the right bracket")
            .defaultValue(new SettingColor(128, 128, 128, 128))
            .visible(()-> !override.get() && mprefix.get())
            .build());


    public BPrefix() {
        super(BananaPlus.MISC, "B+-prefix", "Allows Banana+ prefix for Chat Utils.");
    }


    @Override
    public void onActivate(){
        ChatUtils.registerCustomPrefix("bananaplus.modules", this::getPrefix);

        if (override.get()){
            ChatUtils.registerCustomPrefix("meteordevelopment", this::getPrefix);
        }
        else if (mprefix.get()) {
            ChatUtils.registerCustomPrefix("meteordevelopment", this::getMeteorPrefix);
        }
    }

    @Override
    public void onDeactivate() {
        ChatUtils.unregisterCustomPrefix("bananaplus.modules");
        ChatUtils.unregisterCustomPrefix("meteordevelopment");
    }

    public Text getPrefix() {
        MutableText logo = Text.literal(prefix.get());
        MutableText left = Text.literal(leftBracket.get());
        MutableText right = Text.literal(rightBracket.get());
        MutableText prefix = Text.literal("");

        logo.setStyle(logo.getStyle().withColor(TextColor.fromRgb(prefixColors.get().getPacked())));
        left.setStyle(left.getStyle().withColor(TextColor.fromRgb(leftBracketColor.get().getPacked())));
        right.setStyle(right.getStyle().withColor(TextColor.fromRgb(rightBracketColor.get().getPacked())));

        prefix.append(left);
        prefix.append(logo);
        prefix.append(right);
        prefix.append(" ");

        return prefix;
    }

    public Text getMeteorPrefix() {
        MutableText logo = Text.literal(mprefixname.get());
        MutableText left = Text.literal(mleftBracket.get());
        MutableText right = Text.literal(mrightBracket.get());
        MutableText prefix = Text.literal("");

        logo.setStyle(logo.getStyle().withColor(TextColor.fromRgb(mprefixColors.get().getPacked())));
        left.setStyle(left.getStyle().withColor(TextColor.fromRgb(motorLeftBracketColor.get().getPacked())));
        right.setStyle(right.getStyle().withColor(TextColor.fromRgb(motorRightBracketColor.get().getPacked())));

        prefix.append(left);
        prefix.append(logo);
        prefix.append(right);
        prefix.append(" ");

        return prefix;
    }
}
