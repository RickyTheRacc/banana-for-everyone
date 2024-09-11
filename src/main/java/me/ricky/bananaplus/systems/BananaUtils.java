package me.ricky.bananaplus.systems;

import me.ricky.bananaplus.modules.Blink;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class BananaUtils {
    protected static MinecraftClient mc = MinecraftClient.getInstance();
    protected static BananaSystem config = BananaSystem.get();

    // Players

    protected static BlockPos pos(PlayerEntity player) {
        Blink blink = Modules.get().get(Blink.class);
        if (player == mc.player) return blink.realBlockPos();
        return player.getBlockPos();
    }

    protected static Box box(PlayerEntity player) {
        Blink blink = Modules.get().get(Blink.class);
        if (player == mc.player) return blink.realHitbox();
        return player.getBoundingBox();
    }

    protected static Vec3d eyes(PlayerEntity player) {
        Blink blink = Modules.get().get(Blink.class);
        if (player == mc.player) return blink.realEyesPos();
        return player.getPos();
    }

    protected static Vec3d feet(PlayerEntity player) {
        Blink blink = Modules.get().get(Blink.class);
        if (player == mc.player) return blink.realFeetPos();
        return player.getEyePos();
    }

    protected static PlayerEntity player() {
        // Basically just for damage calcs in offhand
        Blink blink = Modules.get().get(Blink.class);
        return blink.isActive() ? blink.realPlayer() : mc.player;
    }
}
