package bananaplus.mixins;

import bananaplus.modules.BananaPlus;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.checkerframework.checker.signature.qual.MethodDescriptor;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Freecam.class)
public class FreecamMixin{

    @Shadow(remap = false)
    @Final
    private SettingGroup sgGeneral;
    @Shadow(remap = false)
    private Setting<Boolean> rotate;

    private Setting<Boolean> parallelView;
    //private Setting<Boolean> keepWalk;
    //private Setting<Keybind> pauseKeepWalk;

    @Inject(method = "<init>", at=@At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {

        parallelView  = sgGeneral.add(new BoolSetting.Builder()
                .name("parallel-view-B+")
                .description("Rotates the player same way the camera does (good for building).")
                .defaultValue(false)
                .visible(() -> !rotate.get())
                .build()
        );

        /* also wanted 2 add these 2 but idk can't figure out the mixin, ill fix someday.

        keepWalk  = sgGeneral.add(new BoolSetting.Builder()
                .name("keep-walk-B+")
                .description("Keep the ability to walk around but still change the y value of the camera.")
                .defaultValue(false)
                .visible(() -> speedScrollSensitivity.get() == 0)
                .build()
        );

        pauseKeepWalk = sgGeneral.add(new KeybindSetting.Builder()
                .name("pause-keep-walk-B+")
                .description("Pauses keep walk, allowing the player to move freely with the camera.")
                .visible(() -> keepWalk.get())
                .build()
        );
        */
    }

    @Shadow(remap = false)
    public float yaw, pitch;

    private boolean forward, backward, right, left, up, down;

    @Inject(method = "onTick", at = @At("TAIL"), remap = false)
    public void onTick(CallbackInfo info){
        if (parallelView.get() && !rotate.get()){
            pitch = Utils.clamp(pitch, -90, 90);

            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }

        /*
        if(keepWalk.get() && !pauseKeepWalk.get().isPressed()){
            pos.set(mc.player.getX() + realPos.x, pos.y + velY, mc.player.getZ() + realPos.z);
        }
        else {
            pos.set(pos.x + velX, pos.y + velY, pos.z + velZ);
            realPos.set(pos.x - mc.player.getX(), 0, pos.z - mc.player.getZ());
        }
        */
    }

    /*
    @Inject(method = "onActivate", at = @At("HEAD"), remap = false)
    public void onActivate(CallbackInfo info) {
        realPos.set(0, 0, 0);
    }
    */

    //if (!keepWalk.get() ||  (keepWalk.get() && pauseKeepWalk.get().isPressed())) if (cancel) event.cancel();
}
