package bananaplus.mixins.meteor;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Freecam.class)
public class FreecamMixin{
    @Shadow(remap = false) @Final private SettingGroup sgGeneral;
    @Shadow(remap = false) @Final private Setting<Boolean> rotate;
    @Shadow(remap = false) public float yaw, pitch;

    @Unique private Setting<Boolean> parallelView;

    @Inject(method = "<init>", at=@At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        parallelView  = sgGeneral.add(new BoolSetting.Builder()
                .name("parallel-view-B+")
                .description("Rotates the player same way the camera does (good for building).")
                .defaultValue(false)
                .visible(() -> !rotate.get())
                .build()
        );
    }

    @Inject(method = "onTick", at = @At("TAIL"), remap = false)
    public void onTick(CallbackInfo info){
        if (parallelView.get() && !rotate.get()){
            pitch = MathHelper.clamp(pitch, -90, 90);

            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }
}
