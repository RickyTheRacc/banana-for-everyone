package me.ricky.bananaplus.hud;

import me.ricky.bananaplus.BananaPlus;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;

public class TextPresets {
    public static final HudElementInfo<TextHud> INFO = new HudElementInfo<>(BananaPlus.HUD_GROUP, "banana-text", "Displays arbitrary text with Starscript.", TextPresets::create);

    static {
        addPreset("Kills", "Kills: #1{bananaplus.kills}");
        addPreset("Deaths", "Deaths: #1{bananaplus.deaths}");
        addPreset("KDR", "KDR: #1{bananaplus.kdr}");
        addPreset("Highscore", "Highscore: #1{bananaplus.highscore}");
        addPreset("Killstreak", "Killstreak: #1{bananaplus.killstreak}");
        addPreset("Crystals/s", "Crystals/s: #1{bananaplus.crystalsps}");
        addPreset("Ping", "Ping: #1{bananaplus.ping}");
    }

    private static TextHud create() {
        return new TextHud(INFO);
    }

    private static void addPreset(String title, String text) {
        INFO.addPreset(title, textHud -> {
            textHud.text.set(text);
            textHud.updateDelay.set(0);
        });
    }
}