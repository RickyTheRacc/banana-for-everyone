package me.ricky.banana.mixins.minecraft;

import me.ricky.banana.oldmodules.AnchorPlus;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    public void dontJump(CallbackInfo info) {
        AnchorPlus anchorPlus = Modules.get().get(AnchorPlus.class);
        if (anchorPlus.isActive() && anchorPlus.cancelJump) info.cancel();
    }
}
