package me.ricky.banana.mixininterface;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public interface IBlink {
    BlockPos getBlockPos();
    Box getHitbox();
    Vec3d getEyePos();
    Vec3d getFeetPos();

}
