package bananaplus.modules.misc;

import bananaplus.modules.AddModule;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TextColor;

public class BPrefix extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> prefix = sgGeneral.add(new StringSetting.Builder()
            .name("prefix")
            .description("What to be displayed as Banana+ Prefix")
            .defaultValue("Banana+")
            .build());

    private final Setting<SettingColor> prefixColors = sgGeneral.add(new ColorSetting.Builder()
            .name("prefix-color")
            .description("Color display for the prefix")
            .defaultValue(new SettingColor(255,193,0,255))
            .build());

    private final Setting<String> leftBracket = sgGeneral.add(new StringSetting.Builder()
            .name("left-bracket")
            .description("What to be displayed as left bracket for the prefix")
            .defaultValue("[")
            .build());

    private final Setting<String> rightBracket = sgGeneral.add(new StringSetting.Builder()
            .name("right-bracket")
            .description("What to be displayed as right bracket for the prefix")
            .defaultValue("]")
            .build());

    public BPrefix() {
        super(AddModule.MISC, "B+-prefix", "Allows Banana+ prefix for Chat Utils.");
    }

    @Override
    public void onActivate() {
        ChatUtils.registerCustomPrefix("bananaplus.modules", this::getPrefix);
    }

    public LiteralText getPrefix() {
        BaseText logo = new LiteralText(prefix.get());
        LiteralText prefix = new LiteralText("");
        logo.setStyle(logo.getStyle().withColor(TextColor.fromRgb(prefixColors.get().getPacked())));
        prefix.append(leftBracket.get());
        prefix.append(logo);
        prefix.append(rightBracket.get() + " ");
        return prefix;
    }
}
