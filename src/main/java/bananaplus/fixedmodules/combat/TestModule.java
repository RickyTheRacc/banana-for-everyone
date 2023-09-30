package bananaplus.fixedmodules.combat;

import bananaplus.BananaPlus;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class TestModule extends Module {
    public TestModule() {
        super(BananaPlus.FIXED, "guh", "gorp");
    }

    private final List<BlockPos> checkedBlocks = new ArrayList<>();
    private static final BlockPos.Mutable testPos = new BlockPos.Mutable();
    private Box playerBox = new Box(testPos);

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        checkedBlocks.clear();

        playerBox = mc.player.getBoundingBox();
        double maxY = Math.min(playerBox.maxY, playerBox.minY + 1);
        playerBox = playerBox.withMaxY(maxY).contract(0.001);

        for (int i = MathHelper.floor(playerBox.minX); i <= MathHelper.floor(playerBox.maxX); i++) {
            for (int j = MathHelper.floor(playerBox.minY); j <= MathHelper.floor(playerBox.maxY); j++) {
                for (int k = MathHelper.floor(playerBox.minZ); k <= MathHelper.floor(playerBox.maxZ); k++) {
                    testPos.set(i, j, k);
                    checkedBlocks.add(testPos.toImmutable());
                }
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (playerBox != null) event.renderer.box(playerBox, Color.GREEN, Color.GREEN, ShapeMode.Lines, 0);

        checkedBlocks.forEach(pos -> event.renderer.box(pos, Color.RED, Color.RED, ShapeMode.Lines, 0));
    }
}
