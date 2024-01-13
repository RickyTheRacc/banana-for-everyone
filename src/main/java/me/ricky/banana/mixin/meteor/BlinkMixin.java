package me.ricky.banana.mixin.meteor;

import me.ricky.banana.mixininterface.IBlink;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Blink.class, remap = false)
public class BlinkMixin extends Module implements IBlink {
    @Unique private final Box hitbox = new Box(BlockPos.ORIGIN);
    @Unique private final Vec3d eyePos = new Vec3d(0, 0, 0);
    @Unique private final Vec3d feetPos = new Vec3d(0, 0, 0);
    @Unique BlockPos.Mutable blockPos = new BlockPos.Mutable();

    public BlinkMixin() {
        super(Categories.Movement, "blink", "Allows you to essentially teleport while suspending motion updates.");
    }

    @Inject(method = "onActivate", at = @At("TAIL"))
    private void setOldPositions(CallbackInfo ci) {
//        blockPos.set(BlockPos.ofFloored(
//            mc.player.getBlockX(),
//            mc.player.getBlockY() + 0.4,
//            mc.player.getBlockZ()
//        ));
        blockPos.set(mc.player.getBlockPos());

        ((IBox) hitbox).set(
            mc.player.getBoundingBox().minX,
            mc.player.getBoundingBox().minY,
            mc.player.getBoundingBox().minZ,
            mc.player.getBoundingBox().maxX,
            mc.player.getBoundingBox().maxY,
            mc.player.getBoundingBox().maxZ
        );

        ((IVec3d) eyePos).set(
            mc.player.getEyePos().x,
            mc.player.getEyePos().y,
            mc.player.getEyePos().z
        );

        ((IVec3d) feetPos).set(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ()
        );
    }

    @Override
    public BlockPos getBlockPos() {
        return isActive() ? blockPos : mc.player.getBlockPos();
    }

    @Override
    public Box getHitbox() {
        return isActive() ? hitbox : mc.player.getBoundingBox();
    }

    @Override
    public Vec3d getEyePos() {
        return isActive() ? eyePos : mc.player.getEyePos();
    }

    @Override
    public Vec3d getFeetPos() {
        return isActive() ? feetPos : mc.player.getPos();
    }
}
