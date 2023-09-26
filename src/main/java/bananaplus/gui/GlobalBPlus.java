package bananaplus.gui;

import bananaplus.global.GlobalAntiCheat;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.client.gui.screen.Screen;

public class GlobalBPlus extends Tab {

    public static enum Anticheat {
        NoCheatPlus,
        Vanilla
    }


    public GlobalBPlus() {
        super("Banana+");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new BananaPlus(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof BananaPlus;
    }

    private static class BananaPlus extends WindowTabScreen {

        private static Settings settings;

        public BananaPlus(GuiTheme theme, Tab tab) {
            super(theme, tab);

            theme.settings.onActivated();
        }



        @SuppressWarnings("unchecked")
        public Settings getSettings() {
            if (settings != null) return settings;

            settings = new Settings();

            SettingGroup sgAnticheat = settings.createGroup("Anticheats");

            final Setting<GlobalAntiCheat.Anticheat> anticheatSetting = sgAnticheat.add(new EnumSetting.Builder<GlobalAntiCheat.Anticheat>()
                    .name("anti-cheats:  ")
                    .description("The anticheat mode which all modules operate on")
                    .defaultValue(GlobalAntiCheat.Anticheat.NoCheatPlus)
                    .build()
            );
            final Setting<String> yes = sgAnticheat.add(new StringSetting.Builder()
                    .name("Prefix:  ")
                            .description("prefix")
                    .defaultValue("Banana+")
                    .build()
            );
            final Setting<Boolean> yes2 = sgAnticheat.add(new BoolSetting.Builder()
                    .name("Rotations:  ")
                    .description("Rotates")
                    .defaultValue(true)
                    .build()
            );
            return settings;

        }

        @Override
        public void initWidgets() {
            add(theme.settings(getSettings())).expandX();
        }
    }
}
