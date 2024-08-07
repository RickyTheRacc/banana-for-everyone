package me.ricky.banana.hud;

import me.ricky.banana.BananaPlus;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Identifier;

public class LogoHud extends HudElement {
    public static final HudElementInfo<LogoHud> INFO = new HudElementInfo<>(
        BananaPlus.HUD_GROUP, "logo-hud", "Display the Banana+ logo.", LogoHud::new
    );
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("How large the logo should be.")
        .defaultValue(2)
        .min(0.1)
        .sliderRange(0.1,4)
        .build()
    );

    public LogoHud() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        box.setSize(90 * scale.get(), 90 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        GL.bindTexture(new Identifier("bananaplus", "logo.png"));

        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(
            this.x, this.y, getWidth(), getHeight(),
            new Color (255, 255, 255)
        );

        Renderer2D.TEXTURE.render(null);
    }
}