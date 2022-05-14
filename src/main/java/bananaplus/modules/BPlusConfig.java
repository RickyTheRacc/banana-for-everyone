package bananaplus.modules;

import bananaplus.modules.misc.AutoSex;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class BPlusConfig extends Tab {

    public BPlusConfig() {
        super("B+ Config");
    }

    public static BPlusConfigScreen currentScreen;

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return currentScreen = new BPlusConfigScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof BPlusConfigScreen;
    }

    private static final Settings settings = new Settings();

    private static final SettingGroup sgDev = settings.createGroup("Dev");


    //Dev
    public static final Setting<Boolean> testFeatures = sgDev.add(new BoolSetting.Builder()
            .name("test-features")
            .description("Features that need more work or are in a testing phase.")
            .defaultValue(false)
            .onChanged(bool -> BananaPlus.testFeatures())
            .build());

    public static class BPlusConfigScreen extends WindowTabScreen {
        public BPlusConfigScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            settings.onActivated();
        }

        @Override
        public void initWidgets() {
            add(theme.settings(settings)).expandX();
        }

        @Override
        public void tick() {
            super.tick();

            settings.tick(window, theme);
        }
    }
}