package bananaplus.mixins.meteor;

import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.player.SpeedMine;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpeedMine.class, remap = false)
public class SpeedMineMixin extends Module {

    private Setting<Boolean> confirmBreak;

    public SpeedMineMixin(Category category, String name, String description) {
        super(category, name, description);
    }

    @Inject(method = "<init>", at=@At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        SettingGroup sgGeneral = settings.getDefaultGroup();

        // General
        confirmBreak = sgGeneral.add(new BoolSetting.Builder()
                .name("check-breaks")
                .description("Confirm blocks as broken with the server.")
                .defaultValue(true)
                .build()
        );
    }

    @EventHandler
    private void onBlockBreak(BreakBlockEvent event) {
        if (confirmBreak.get()) {
            if (event.blockPos != null) {
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, event.blockPos, Direction.DOWN));
            }
        }
    }
}
