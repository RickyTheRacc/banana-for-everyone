package me.ricky.banana.mixin.minecraft;

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
    // Fuck you earthcomputer
    @ModifyVariable(method = "jump", at = @At("STORE"))
    private float setJumpYaw(float original) {
        return getJumpYaw();
    }

    @Unique
    private float getJumpYaw() {
        float yaw = mc.player.getYaw();

        Sprint sprint = Modules.get().get(Sprint.class);
        if (sprint.isActive() && sprint.allDirections.get()) {
            if (mc.player.forwardSpeed < 0) {
                yaw += 180;
                if (mc.player.sidewaysSpeed != 0) {
                    yaw += mc.player.sidewaysSpeed > 0 ? -135 : 135;
                }
            } else {
                if (mc.player.sidewaysSpeed != 0) {
                    yaw += mc.player.sidewaysSpeed > 0 ? -45 : 45;
                }
            }

            if (yaw >= 360) yaw -= 360;
        }

        return (float) Math.toRadians(yaw);
    }
}
