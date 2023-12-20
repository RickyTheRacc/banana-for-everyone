package bananaplus.mixins.meteor;

import bananaplus.system.BananaConfig;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@SuppressWarnings("rawtypes")
@Mixin(value = Systems.class, remap = false)
public abstract class SystemsMixin {
    @Final @Shadow(remap = false) private static Map<Class<? extends System>, System<?>> systems;
    @Shadow(remap = false) private static System<?> add(System<?> system) {
        systems.put(system.getClass(), system);
        MeteorClient.EVENT_BUS.subscribe(system);
        system.init();
        return system;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private static void injectBananaSystem(CallbackInfo ci) {
        System<?> bananaSystem = add(new BananaConfig());
        bananaSystem.init();
        bananaSystem.load();
    }
}
