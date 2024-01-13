package me.ricky.banana.mixininterface;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public interface IBlink {
    BlockPos realBlockPos();
    Box realHitbox();
    Vec3d realEyesPos();
    Vec3d realFeetPos();
}