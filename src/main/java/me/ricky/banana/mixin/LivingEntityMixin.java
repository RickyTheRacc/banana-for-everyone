package me.ricky.banana.mixin;

import me.ricky.banana.modules.movement.Sprint;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    // This is actually correct, MCdev just doesn't know what it's talking about
    @ModifyVariable(method = "jump", at = @At("STORE"))
    private float setJumpYaw(float original) {
        return getJumpYaw();
    }

    @Unique
    private float getJumpYaw() {
        float yaw = mc.player.getYaw();

        Sprint sprint = Modules.get().get(Sprint.class);
        if (sprint.isActive() && sprint.allDirections.get()) {
            float forwardMovement = mc.player.input.movementForward;
            float sidewaysMovement = mc.player.input.movementSideways;

            yaw += (float) Math.toDegrees(-Math.atan2(sidewaysMovement, forwardMovement));
            yaw = (yaw + 180) % 360 - 180;   // Correct to between 180 and -180
        }

        return (float) Math.toRadians(yaw);
    }
}
