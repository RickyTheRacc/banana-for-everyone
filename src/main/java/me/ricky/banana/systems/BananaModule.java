package me.ricky.banana.systems;

import me.ricky.banana.modules.movement.Blink;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.world.Dir;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BananaModule extends Module {
    protected BananaSystem config = BananaSystem.get();

    public BananaModule(Category category, String name, String description) {
        super(category, name, description);
    }

    // Players

    protected BlockPos pos(PlayerEntity player) {
        Blink blink = Modules.get().get(Blink.class);
        if (player == mc.player) return blink.realBlockPos();
        return player.getBlockPos();
    }

    protected Box box(PlayerEntity player) {
        Blink blink = Modules.get().get(Blink.class);
        if (player == mc.player) return blink.realHitbox();
        return player.getBoundingBox();
    }

    protected Vec3d eyes(PlayerEntity player) {
        Blink blink = Modules.get().get(Blink.class);
        if (player == mc.player) return blink.realEyesPos();
        return player.getPos();
    }

    protected Vec3d feet(PlayerEntity player) {
        Blink blink = Modules.get().get(Blink.class);
        if (player == mc.player) return blink.realFeetPos();
        return player.getEyePos();
    }

    // Rendering

    public double textScale(Vector3d pos) {
        double denom = pos.distance(
            mc.gameRenderer.getCamera().getPos().x,
            mc.gameRenderer.getCamera().getPos().y,
            mc.gameRenderer.getCamera().getPos().z
        ) / config.divisor.get();

        return Math.clamp(config.textScale.get() / denom, config.minScale.get(), config.maxScale.get());
    }

    protected Map<BlockPos, Integer> prettySides(List<BlockPos> list) {
        Map<BlockPos, Integer> sides = new HashMap<>();

        for (BlockPos pos: list) {
            int excludeSides = 0;

            for (Direction direction: Direction.values()) {
                if (!list.contains(pos.offset(direction))) continue;
                excludeSides |= Dir.get(direction);
            }

            sides.put(pos, excludeSides);
        }

        return sides;
    }

    // Misc

//    protected double tickPassed() {
//        return config.tpsSync.get() ? TickRate.INSTANCE.getTickRate() / 20.0 : 1.0;
//    }
}
