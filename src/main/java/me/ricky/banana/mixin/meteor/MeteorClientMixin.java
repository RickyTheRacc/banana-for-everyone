package me.ricky.banana.mixin.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MeteorClient.class, remap = false)
public abstract class MeteorClientMixin {
    @Inject(method = "onOpenScreen", at = @At("TAIL"))
    public void thing(OpenScreenEvent event, CallbackInfo ci) {
        if (!(event.screen instanceof HudEditorScreen)) return;
        MeteorClient.mc.options.hudHidden = false;
    }
}
