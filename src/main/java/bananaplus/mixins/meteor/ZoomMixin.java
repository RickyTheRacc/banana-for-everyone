package bananaplus.mixins.meteor;

import bananaplus.modules.BananaPlus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.render.Zoom;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zoom.class)
public class ZoomMixin extends Module{

    public ZoomMixin() {
        super(BananaPlus.MISC, "zoom", "is there a better way 2 do this? idk");
    }

    @Shadow(remap = false)
    @Final
    private SettingGroup sgGeneral;

    private Setting<Boolean> f1;

    @Inject(method = "<init>", at=@At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {

        f1 = sgGeneral.add(new BoolSetting.Builder()
                .name("f1")
                .description("Toggles the Heads-Up Display.")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "onActivate", at = @At("TAIL"), remap = false)
    public void onActivate(CallbackInfo info){
        if (f1.get()){
            mc.options.hudHidden = true;
        }
    }

    @Override
    public void onDeactivate() {
        if (f1.get()){
            mc.options.hudHidden = false;
        }
    }
}
