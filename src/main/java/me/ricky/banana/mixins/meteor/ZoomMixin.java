package me.ricky.banana.mixins.meteor;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.render.Zoom;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zoom.class)
public class ZoomMixin extends Module{
    @Shadow(remap = false) @Final private SettingGroup sgGeneral;

    @Unique private Setting<Boolean> toggleHud;
    @Unique private boolean wasHudHidden;

    public ZoomMixin() {
        super(Categories.Render, "zoom", "Zooms your view.");
    }

    @Inject(method = "<init>", at=@At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        toggleHud = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-hud")
            .description("Toggles the Heads-Up Display.")
            .defaultValue(false)
            .build()
        );
    }

    @Inject(method = "onActivate", at = @At("TAIL"), remap = false)
    public void onActivate(CallbackInfo info){
        if (toggleHud.get()){
            wasHudHidden = mc.options.hudHidden;
            mc.options.hudHidden = true;
        }
    }

    @Override
    public void onDeactivate() {
        if (toggleHud.get()){
            mc.options.hudHidden = wasHudHidden;
        }
    }
}
