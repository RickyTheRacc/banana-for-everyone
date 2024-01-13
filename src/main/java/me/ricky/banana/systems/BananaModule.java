package me.ricky.banana.systems;

import me.ricky.banana.mixininterface.IBlink;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public interface BananaModule {
    BananaConfig config = BananaConfig.get();
    IBlink blink = (IBlink) Modules.get().get(Blink.class);

    default BlockPos pos(PlayerEntity player) {
        if (player != mc.player) return player.getBlockPos();
        return blink.getBlockPos();
    }
}
