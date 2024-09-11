package me.ricky.bananaplus.systems;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.client.gui.screen.Screen;

public class BananaTab extends Tab {
    public BananaTab() {
        super("Banana+");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new BananaPlusScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof BananaPlusScreen;
    }

    private static class BananaPlusScreen extends WindowTabScreen {
        private final Settings settings;

        public BananaPlusScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
            settings = BananaSystem.get().settings;
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

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(BananaSystem.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(BananaSystem.get());
        }
    }
}