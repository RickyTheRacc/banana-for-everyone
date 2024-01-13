package me.ricky.banana.mixin.meteor;

import me.ricky.banana.mixininterface.IBlink;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(value = Blink.class, remap = false)
public abstract class BlinkMixin implements IBlink {
    @Unique private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    @Unique private final Box hitbox = new Box(BlockPos.ORIGIN);
    @Unique private final Vec3d eyePos = new Vec3d(0, 0, 0);
    @Unique private final Vec3d feetPos = new Vec3d(0, 0, 0);

    @Inject(method = "onActivate", at = @At("TAIL"))
    private void setOldPositions(CallbackInfo ci) {
        blockPos.set(
            mc.player.getBlockX(),
            mc.player.getBlockY(),
            mc.player.getBlockZ()
        );

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
    public BlockPos realBlockPos() {
        // Since sometimes with things like standing on echests or pearling
        // the pos is technically like 0.1 below our actual feet level pos

        return BlockPos.ofFloored(
            blockPos.getX(),
            blockPos.getY() + 0.4,
            blockPos.getZ()
        );
    }

    @Override
    public Box realHitbox() {
        return hitbox;
    }

    @Override
    public Vec3d realEyesPos() {
        return eyePos;
    }

    @Override
    public Vec3d realFeetPos() {
        return feetPos;
    }
}
