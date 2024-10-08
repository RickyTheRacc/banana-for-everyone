package me.ricky.bananaplus.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.ricky.bananaplus.modules.Sprint;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends Entity {
    @Shadow protected abstract boolean canSprint();
    @Shadow public abstract boolean isSubmergedInWater();

    @Unique private Sprint sprint;

    public ClientPlayerEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyVariable(method = "tickMovement", at = @At("STORE"), name = "bl8")
    private boolean canPlayerSprint(boolean bl8) {
        if (getSprint().isActive()) return !getSprint().shouldAutoSprint() || !this.canSprint();

        return bl8;
    }

    // The local here does actually exist, MCdev is just being dumb

    @ModifyVariable(method = "tickMovement", at = @At("STORE"), name = "bl9")
    private boolean shouldPlayerStopSprinting(boolean bl9, @Local(ordinal = 4) boolean bl8) {
        if (getSprint().isActive() && getSprint().preventStop.get()) return bl8 || this.isTouchingWater() && this.isSubmergedInWater();

        return bl9;
    }

    @Unique
    private Sprint getSprint() {
        if (sprint == null) sprint = Modules.get().get(Sprint.class);
        return sprint;
    }
}
