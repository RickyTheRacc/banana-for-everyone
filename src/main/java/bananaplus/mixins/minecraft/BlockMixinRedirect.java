package bananaplus.mixins.minecraft;

import com.bawnorton.mixinsquared.TargetHandler;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(value = Block.class, priority = 9999)
public abstract class BlockMixinRedirect {
//    @TargetHandler(
//        mixin = "meteordevelopment.meteorclient.mixin.BlockMixin",
//        name = "getSlipperiness"
//    )
//    @Redirect(
//        method = "@MixinSquared:Handler",
//        at = @At("")
//    )
//    private float getSlipperiness(float original) {
//        Modules modules = Modules.get();
//        if (modules == null || !modules.isActive(NoSlow.class)) return original;
//        if (!modules.get(NoSlow.class).slimeBlock()) return original;
//        return ((Object) this == Blocks.SLIME_BLOCK) ? 0.6F : original;
//    }
}
