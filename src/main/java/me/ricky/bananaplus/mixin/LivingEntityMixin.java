package me.ricky.bananaplus.mixin;

import me.ricky.bananaplus.modules.Sprint;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@SuppressWarnings("ConstantConditions")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique private Sprint sprint;

    @ModifyVariable(method = "jump", name = "g", ordinal = 1, at = @At("STORE"))
    private float setJumpYaw(float original) {
        if (!getSprint().isActive()) return original;

        float yaw = mc.player.getYaw();

        if (getSprint().allDirections.get()) {
            float forwardMovement = mc.player.input.movementForward;
            float sidewaysMovement = mc.player.input.movementSideways;

            yaw += (float) Math.toDegrees(-Math.atan2(sidewaysMovement, forwardMovement));
            yaw = (yaw + 180) % 360 - 180;   // Correct to between 180 and -180
        }

        return (float) Math.toRadians(yaw);
    }

    @Unique
    private Sprint getSprint() {
        if (sprint == null) sprint = Modules.get().get(Sprint.class);
        return sprint;
    }

}
