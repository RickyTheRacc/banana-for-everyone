package me.ricky.banana.mixin.meteor;

import me.ricky.banana.systems.BananaSystem;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Systems.class, remap = false)
public abstract class SystemsMixin {
    @Shadow private static System<?> add(System<?> system) {
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("HEAD"))
    private static void injectBananaSystem(CallbackInfo ci) {
        System<?> bananaSystem = add(new BananaSystem());
        bananaSystem.init();
        bananaSystem.load();
    }
}