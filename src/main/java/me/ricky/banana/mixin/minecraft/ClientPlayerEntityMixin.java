package me.ricky.banana.mixin.minecraft;

import me.ricky.banana.modules.movement.Sprint;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Redirect(method = "tickMovement()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;hasForwardMovement()Z"))
    private boolean canPlayerSprint(@NotNull Input instance) {
        Sprint sprint = Modules.get().get(Sprint.class);
        if (!sprint.isActive()) return instance.hasForwardMovement();

        return sprint.canSprint();
    }
}
