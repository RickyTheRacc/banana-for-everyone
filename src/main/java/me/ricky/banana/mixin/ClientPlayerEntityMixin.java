package me.ricky.banana.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.ricky.banana.modules.movement.Sprint;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends Entity {
    public ClientPlayerEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow protected abstract boolean canSprint();
    @Shadow public abstract boolean isSubmergedInWater();

    @ModifyVariable(method = "tickMovement", at = @At("STORE"), name = "bl8")
    private boolean canPlayerSprint(boolean bl8) {
        Sprint sprint = Modules.get().get(Sprint.class);
        if (sprint.isActive()) return !sprint.canSprint() || !this.canSprint();

        return bl8;
    }

    @ModifyVariable(method = "tickMovement", at = @At("STORE"), name = "bl9")
    private boolean shouldPlayerStopSprinting(boolean bl9, @Local(ordinal = 4) boolean bl8) {
        Sprint sprint = Modules.get().get(Sprint.class);
        if (sprint.isActive() && sprint.preventStop.get()) return bl8 || this.isTouchingWater() && this.isSubmergedInWater();

        return bl9;
    }
}
