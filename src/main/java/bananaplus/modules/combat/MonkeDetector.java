package bananaplus.modules.combat;

import bananaplus.modules.AddModule;
import bananaplus.utils.BPlusEntityUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class MonkeDetector extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Lines)
        .build());

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 255, 255, 75))
        .build());

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build());

    public MonkeDetector() {
        super(AddModule.COMBAT, "monke-detector", "Checks if the CA target is not burrowed, and isn't surrounded. (To be paired with Banana Bomber)");
    }

    private PlayerEntity target;
    private BlockPos targetPos;

    @Override
    public void onActivate() {
        target = null;
        targetPos = null;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        BananaBomber BBomber = Modules.get().get(BananaBomber.class);
        target = BBomber.getPlayerTarget();
            if (target != null && !BPlusEntityUtils.isSurrounded(target, BPlusEntityUtils.BlastResistantType.Any) && !BPlusEntityUtils.isBurrowed(target, BPlusEntityUtils.BlastResistantType.Any) && target.isAlive()) {
                targetPos = target.getBlockPos();
            } else {
                target = null;
                targetPos = null;
            }
        }


    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (target != null && targetPos != null) {
            event.renderer.box(targetPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}
