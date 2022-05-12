package bananaplus.modules.hud;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;

public class CoordinatesHud extends HudElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> xSpoof = sgGeneral.add(new IntSetting.Builder()
            .name("X-Spoof")
            .description("Changes your X by this amount.")
            .defaultValue(0)
            .range(-2147483648, 2147483647)
            .sliderRange(-2147483648, 2147483647)
            .build()
    );

    private final Setting<Integer> ySpoof = sgGeneral.add(new IntSetting.Builder()
            .name("Y-Spoof")
            .description("Changes your Y by this amount.")
            .defaultValue(0)
            .range(-2147483648, 2147483647)
            .sliderRange(-2147483648, 2147483647)
            .build()
    );

    private final Setting<Integer> zSpoof = sgGeneral.add(new IntSetting.Builder()
            .name("Z-Spoof")
            .description("Changes your Z by this amount.")
            .defaultValue(0)
            .range(-2147483648, 2147483647)
            .sliderRange(-2147483648, 2147483647)
            .build()
    );

    private final String left1 = "Pos: ";
    private double left1Width;
    private String right1;

    private String left2;
    private double left2Width;
    private String right2;

    public CoordinatesHud(HUD hud) {
        super(hud, "Coords Spoofer", "Spoofs your coordinates in the world.");
    }

    @Override
    public void update(HudRenderer renderer) {
        left1Width = renderer.textWidth(left1);
        left2 = null;

        if (isInEditor()) {
            right1 = "0,0 0,0 0,0";
            box.setSize(left1Width + renderer.textWidth(right1), renderer.textHeight() * 2 + 2);
            return;
        }

        Freecam freecam = Modules.get().get(Freecam.class);

        double x1 = freecam.isActive() ? mc.gameRenderer.getCamera().getPos().x : mc.player.getX();
        double y1 = freecam.isActive() ? mc.gameRenderer.getCamera().getPos().y - mc.player.getEyeHeight(mc.player.getPose()) : mc.player.getY();
        double z1 = freecam.isActive() ? mc.gameRenderer.getCamera().getPos().z : mc.player.getZ();

        right1 = String.format("%.1f %.1f %.1f", x1 + xSpoof.get(),y1 + ySpoof.get(),z1 + zSpoof.get());

        switch (PlayerUtils.getDimension()) {
            case Overworld -> {
                left2 = "Nether Pos: ";
                right2 = String.format("%.1f %.1f %.1f", (x1 + xSpoof.get()) / 8.0, y1 + ySpoof.get(), (z1 + zSpoof.get() / 8.0));
            }
            case Nether -> {
                left2 = "Overworld Pos: ";
                right2 = String.format("%.1f %.1f %.1f", (x1 + xSpoof.get()) * 8.0, y1 + ySpoof.get(), (z1 + zSpoof.get()) * 8.0);
            }
        }

        double width = left1Width + renderer.textWidth(right1);

        if (left2 != null) {
            left2Width = renderer.textWidth(left2);
            width = Math.max(width, left2Width + renderer.textWidth(right2));
        }

        box.setSize(width, renderer.textHeight() * 2 + 2);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (left2 != null) {
            renderer.text(left2, x, y, hud.primaryColor.get());
            renderer.text(right2, x + left2Width, y, hud.secondaryColor.get());
        }

        double xOffset = box.alignX(left1Width + renderer.textWidth(right1));
        double yOffset = renderer.textHeight() + 2;

        renderer.text(left1, x + xOffset, y + yOffset, hud.primaryColor.get());
        renderer.text(right1, x + xOffset + left1Width, y + yOffset, hud.secondaryColor.get());
    }
}