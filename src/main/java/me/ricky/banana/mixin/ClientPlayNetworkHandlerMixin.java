package me.ricky.banana.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    // Minehut's NPC plugin for some reason sends player updates for all the NPCs, which have invalid names
    // and cause the client to log a warning for each one. This is a temporary fix until they fix their shit

    @Redirect(method = "onPlayerList", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void preventLogSpam(Logger instance, String s, Object o1, Object o2) {}
}
