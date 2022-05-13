package bananaplus.modules.combat;

import bananaplus.modules.BananaPlus;
import bananaplus.utils.BPlusEntityUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class CityESPPlus extends Module {

    public CityESPPlus() { super(BananaPlus.COMBAT, "city-esp+", "Displays more blocks that can be broken in order to city another player."); }

    private final SettingGroup sgRender = settings.createGroup("Render");

    // Render
    private final Setting<Double> range = sgRender.add(new DoubleSetting.Builder()
            .name("range")
            .description("The distance which to find the enemy.")
            .defaultValue(6)
            .min(0)
            .build());

    private final Setting<Boolean> prioBurrowed = sgRender.add(new BoolSetting.Builder()
            .name("prioritise-burrow")
            .description("Will prioritise rendering the burrow block.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> noRenderSurrounded = sgRender.add(new BoolSetting.Builder()
            .name("not-surrounded")
            .description("Will not render if the target is not surrounded.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> avoidSelf = sgRender.add(new BoolSetting.Builder()
            .name("avoid-self")
            .description("Will avoid targeting self surround.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> lastResort = sgRender.add(new BoolSetting.Builder()
            .name("last-resort")
            .description("Will try to target your own surround as final option.")
            .defaultValue(true)
            .visible(avoidSelf::get)
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the rendering.")
            .defaultValue(new SettingColor(230, 0, 255, 5))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the rendering.")
            .defaultValue(new SettingColor(250, 0, 255, 255))
            .build());

    public BlockPos target;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        PlayerEntity targetEntity = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);

        if (TargetUtils.isBadTarget(targetEntity, range.get())) {
            target = null;
        } else if (prioBurrowed.get() && BPlusEntityUtils.isBurrowed(targetEntity, BPlusEntityUtils.BlastResistantType.Mineable)) {
            target = targetEntity.getBlockPos();
        } else if (noRenderSurrounded.get() && !BPlusEntityUtils.isSurrounded(targetEntity, BPlusEntityUtils.BlastResistantType.Any)) {
            target = null;
        } else if (avoidSelf.get()) {
            target = BPlusEntityUtils.getTargetBlock(targetEntity);
                if (target == null && lastResort.get()) target = BPlusEntityUtils.getCityBlock(targetEntity);
        } else target = BPlusEntityUtils.getCityBlock(targetEntity);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (target == null) return;
        event.renderer.box(target, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
