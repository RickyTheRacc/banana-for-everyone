package me.ricky.banana.mixins.minecraft;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.mixin.BlockMixin;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(value = Block.class, priority = 9999)
public abstract class BlockMixinRedirect {
    // For once I actually figure out mixinsquared

//    @TargetHandler(
//        mixin = "meteordevelopment.meteorclient.mixin.BlockMixin",
//        name = "getSlipperiness"
//    )
//    @Redirect(
//        method = "@MixinSquared:Handler",
//        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getSlipperiness()F")
//    )
//    private float getSlipperiness(float original) {
//        Modules modules = Modules.get();
//        if (modules == null || !modules.isActive(NoSlow.class)) return original;
//        if (!modules.get(NoSlow.class).slimeBlock()) return original;
//        return ((Object) this == Blocks.SLIME_BLOCK) ? 0.6F : original;
//    }
}
