package me.ricky.banana.mixin.minecraft;

import me.ricky.banana.oldmodules.AnchorPlus;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    public void cancelJump(CallbackInfo info) {
        AnchorPlus anchorPlus = Modules.get().get(AnchorPlus.class);
        if (anchorPlus.isActive() && anchorPlus.cancelJump) info.cancel();
    }
}
