package bananaplus.modules.hud;

import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Identifier;

import java.util.Calendar;

public class HudLogo extends HudElement {
    public enum Logo {
        Default,
        British,
        Festive,
        Spooky,
        Mexican,
        Irish,
        Patriot,
        Pride,
        Swedish
    }

    public enum Default {
        Default,
        Plus2,
        Plus3,
        Circle
    }

    public enum Halloween {
        Default,
        Ghosts,
        Pumpkin
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    // General
    private final Setting<Logo> logo = sgGeneral.add(new EnumSetting.Builder<Logo>()
            .name("logo-image")
            .description("Which image to display as the logo.")
            .defaultValue(Logo.Default)
            .build()
    );

    private final Setting<Boolean> events = sgGeneral.add(new BoolSetting.Builder()
            .name("events")
            .description("Use seasonal edits of the logo for holidays!")
            .defaultValue(true)
            .build()
    );

    private final Setting<Default> normal = sgGeneral.add(new EnumSetting.Builder<Default>()
            .name("default-mode")
            .description("Which image to display as the logo.")
            .defaultValue(Default.Default)
            .build()
    );

    private final Setting<Halloween> halloween = sgGeneral.add(new EnumSetting.Builder<Halloween>()
            .name("spooky-mode")
            .description("Which image to display for the spooky month.")
            .defaultValue(Halloween.Default)
            .visible (() -> events.get() || logo.get() == Logo.Spooky)
            .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("How large the logo should be.")
            .defaultValue(2)
            .min(0.1)
            .sliderRange(0,4)
            .build()
    );


    public HudLogo(HUD hud) {
        super(hud, "banana+-logo", "Displays the Banana+ logo");
    }


    // Normal Logos
    private static final Identifier LOGO = new Identifier("textures", "logo.png");
    private static final Identifier LOGO2 = new Identifier("textures", "logo2.png");
    private static final Identifier LOGO3 = new Identifier("textures", "logo3.png");
    private static final Identifier CIRCLE = new Identifier("textures", "circle.png");


    // Seasonal Logos
    private static final Identifier BRITISH = new Identifier("textures", "british.png");
    private static final Identifier CHRISTMAS = new Identifier("textures", "christmas.png");
    private static final Identifier HALLOWEEN = new Identifier("textures", "halloween1.png");
    private static final Identifier HALLOWEEN2 = new Identifier("textures", "halloween2.png");
    private static final Identifier HALLOWEEN3 = new Identifier("textures", "halloween3.png");
    private static final Identifier MEXICO = new Identifier("textures", "mexican.png");
    private static final Identifier PATRICK = new Identifier("textures", "patrick.png");
    private static final Identifier PATRIOT = new Identifier("textures", "patriot.png");
    private static final Identifier PRIDE = new Identifier("textures", "pride.png");
    private static final Identifier SWEDEN = new Identifier("textures", "swedish.png");

    public Identifier getNormal() {
        if (normal.get() == Default.Default) return LOGO;
        if (normal.get() == Default.Plus2) return LOGO2;
        if (normal.get() == Default.Plus3) return LOGO3;
        return CIRCLE;
    }

    public Identifier getSpooky() {
        if (halloween.get() == Halloween.Default) return HALLOWEEN;
        if (halloween.get() == Halloween.Pumpkin) return HALLOWEEN2;
        return HALLOWEEN3;
    }

    public Identifier getDefault() {
        if (logo.get() == Logo.British) return BRITISH;
        if (logo.get() == Logo.Festive) return CHRISTMAS;
        if (logo.get() == Logo.Spooky) return getSpooky();
        if (logo.get() == Logo.Mexican) return MEXICO;
        if (logo.get() == Logo.Irish) return PATRICK;
        if (logo.get() == Logo.Patriot) return PATRIOT;
        if (logo.get() == Logo.Pride) return PRIDE;
        if (logo.get() == Logo.Swedish) return SWEDEN;
        return getNormal();
    }

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

        if (events.get()) {
            // June
            if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.JUNE && Calendar.getInstance().get(Calendar.DATE) == 6) GL.bindTexture(SWEDEN); // Sweden Day
            else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.JUNE && Calendar.getInstance().get(Calendar.DATE) == 12) GL.bindTexture(BRITISH); // British National Day
            else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.JUNE) GL.bindTexture(PRIDE); // Pride Month
            // July
            else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.JULY && Calendar.getInstance().get(Calendar.DATE) == 4) GL.bindTexture(PATRIOT); // Independence Day
            // March
            else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.MARCH && Calendar.getInstance().get(Calendar.DATE) == 17) GL.bindTexture(PATRICK); // St. Patrick's Day
            // September
            else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.SEPTEMBER && Calendar.getInstance().get(Calendar.DATE) == 16) GL.bindTexture(MEXICO); // Mexican Independence
            // October
            else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.OCTOBER) GL.bindTexture(getSpooky()); // Spooky Month
            // December
            else if (Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER && Calendar.getInstance().get(Calendar.DATE) == 25) GL.bindTexture(CHRISTMAS); // Christmas
            // Default if it's not a holiday
            else GL.bindTexture(getDefault());
        }

        if (!events.get()) GL.bindTexture(getDefault());

        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, w, h, color);
        Renderer2D.TEXTURE.render(null);
    }
}
