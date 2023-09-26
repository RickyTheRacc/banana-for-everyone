package bananaplus.hud;

import bananaplus.BananaPlus;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;

public class TextPresets {
    public static final HudElementInfo<TextHud> INFO = new HudElementInfo<>(
            BananaPlus.HUD_GROUP, "banana-text", "Displays arbitrary text with Starscript.", TextPresets::create
    );

    static {
        addPreset("Kills", "Kills: #1{banana.kills}");
        addPreset("Deaths", "Deaths: #1{banana.deaths}");
        addPreset("KDR", "KDR: #1{banana.kdr}");
        addPreset("Highscore", "Highscore: #1{banana.highscore}");
        addPreset("Killstreak", "Killstreak: #1{banana.killstreak}");
        addPreset("Crystals/s", "Crystals/s: #1{banana.crystalsps}");
    }

    private static TextHud create() {
        return new TextHud(INFO);
    }

    private static void addPreset(String title, String text) {
        INFO.addPreset(title, textHud -> {
            if (text != null) textHud.text.set(text);
            textHud.updateDelay.set(0);
        });
    }
}