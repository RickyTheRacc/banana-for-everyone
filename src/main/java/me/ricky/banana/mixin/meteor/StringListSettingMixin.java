package me.ricky.banana.mixin.meteor;

import meteordevelopment.meteorclient.settings.StringListSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = StringListSetting.class, remap = false)
public abstract class StringListSettingMixin {
    // Remove the horizontal separator between the last line and the add button
    // It's objectively bad design and seasnail can cope and seethe
    
    @Redirect(method = "fillTable", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 0))
    private static boolean removeSeparator(List<?> instance) {
        return true;
    }
}
