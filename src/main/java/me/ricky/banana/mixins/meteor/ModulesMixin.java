package me.ricky.banana.mixins.meteor;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value = Modules.class, remap = false)
public abstract class ModulesMixin {
    // Remove Trails
    @Redirect(method = "initRender",
        at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/Modules;add(Lmeteordevelopment/meteorclient/systems/modules/Module;)V", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Trail;<init>()V"))
    )
    private void removeTrails(Modules instance, Module _module) {}

    // Remove Offhand Crash
    @Redirect(method = "initPlayer",
        at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/Modules;add(Lmeteordevelopment/meteorclient/systems/modules/Module;)V", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/player/OffhandCrash;<init>()V"))
    )
    private void removeOffhandCrash(Modules instance, Module _module) {}

    // Remove Click TP
    @Redirect(method = "initMovement",
        at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/Modules;add(Lmeteordevelopment/meteorclient/systems/modules/Module;)V", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/movement/ClickTP;<init>()V"))
    )
    private void removeClickTP(Modules instance, Module _module) {}

    // Remove Mount Bypass
    @Redirect(method = "initWorld",
        at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/Modules;add(Lmeteordevelopment/meteorclient/systems/modules/Module;)V", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/world/MountBypass;<init>()V"))
    )
    private void removeMountBypass(Modules instance, Module _module) {}

    // Remove High Jump
    @Redirect(method = "initMovement",
        at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/Modules;add(Lmeteordevelopment/meteorclient/systems/modules/Module;)V", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/movement/HighJump;<init>()V"))
    )
    private void removeHighJump(Modules instance, Module _module) {}

    // Remove High Jump
//    @Redirect(method = "initMovement",
//        at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/Modules;add(Lmeteordevelopment/meteorclient/systems/modules/Module;)V", ordinal = 0),
//        slice = @Slice(from = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/movement/Slippy;<init>()V"))
//    )
//    private void removeSlippy(Modules instance, Module _module) {}
}
