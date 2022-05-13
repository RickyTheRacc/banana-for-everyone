package bananaplus.modules.hud;

import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Identifier;

import java.util.Calendar;

public class HudLogo extends HudElement {

    public HudLogo(HUD hud) {
        super(hud, "banana+-logo", "Displays the Banana+ logo");
    }

    public enum Mode {Event, Basic, Circled}

    private static final Identifier LOGO = new Identifier("textures", "logo.png");
    private static final Identifier LOGOC = new Identifier("textures", "circle.png");
    private static final Identifier LOGOCHRIS = new Identifier("textures", "xmas.png");
    private static final Identifier LOGOHALLO = new Identifier("textures", "ween.png");
    private static final Identifier SWEDEN = new Identifier("textures", "sweden.png");
    private static final Identifier PRIDE = new Identifier("textures", "pride.png");
    private static final Identifier BRI = new Identifier("textures", "bri.png");
    private static final Identifier MEX = new Identifier("textures", "mex.png");
    private static final Identifier USA = new Identifier("textures", "usa.png");
    private static final Identifier PAT = new Identifier("textures", "patrick.png");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Mode> logo = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Logo")
            .description("Which logo to use for the hud")
            .defaultValue(Mode.Circled)
            .build());

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the logo")
            .defaultValue(3)
            .min(0.1)
            .sliderMin(0.1)
            .sliderMax(4)
            .build());


    @Override
    public void update(HudRenderer renderer) {
        box.setSize(90 * scale.get(), 90 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        if (!Utils.canUpdate()) return;

        double x = box.getX();
        double y = box.getY();
        int w = (int) box.width;
        int h = (int) box.height;
        Color color = new Color (255, 255, 255);

        if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.OCTOBER && logo.get() == Mode.Event) //halloween
        {
            GL.bindTexture(LOGOHALLO);
        }
        else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER && logo.get() == Mode.Event) GL.bindTexture(LOGOCHRIS); //xmas
        else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.JUNE && Calendar.getInstance().get(Calendar.DATE) == 6 && logo.get() == Mode.Event) GL.bindTexture(SWEDEN); //sweden
        else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.JUNE && Calendar.getInstance().get(Calendar.DATE) == 12 && logo.get() == Mode.Event) GL.bindTexture(SWEDEN); //brits
        else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.JUNE && logo.get() == Mode.Event) GL.bindTexture(PRIDE); //pride
        else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.SEPTEMBER && Calendar.getInstance().get(Calendar.DATE) == 16 && logo.get() == Mode.Event) GL.bindTexture(MEX); //mexico
        else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.JULY && Calendar.getInstance().get(Calendar.DATE) == 4 && logo.get() == Mode.Event) GL.bindTexture(SWEDEN); //usa

        else if (logo.get() == Mode.Event) GL.bindTexture(LOGO);
        else if (logo.get() == Mode.Basic) GL.bindTexture(LOGO);
        else GL.bindTexture(LOGOC);

        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, w, h, color);
        Renderer2D.TEXTURE.render(null);
    }
}
