package bananaplus.mixins.meteor;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoRespawn;
import meteordevelopment.meteorclient.systems.modules.render.WaypointsModule;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;


@Mixin(AutoRespawn.class)
public class AutoRespawnMixin extends Module{

    @Unique private Setting<Boolean> autoRekit;
    @Unique private Setting<Boolean> chatInfo;
    @Unique private Setting<String> kitName;

    public AutoRespawnMixin() {
        super(Categories.Player, "auto-respawn", "Automatically respawns after death.");
    }

    @Inject(method = "<init>", at=@At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        SettingGroup sgGeneral = settings.getDefaultGroup();

        // General

        autoRekit = sgGeneral.add(new BoolSetting.Builder()
            .name("rekit")
            .description("Whether to automatically get a kit after dying.")
            .defaultValue(false)
            .build()
        );

        chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Whether to send info about rekitting.")
            .defaultValue(false)
            .visible(autoRekit::get)
            .build()
        );

        kitName= sgGeneral.add(new StringSetting.Builder()
            .name("kit-name")
            .description("The name of your kit.")
            .defaultValue("")
            .visible(autoRekit::get)
            .build()
        );
    }

    /**
     * @author RickyTheRacc
     * @reason Add rekit settings
     */
    @EventHandler(priority = 100)
    @Overwrite(remap = false)
    private void onOpenScreenEvent(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;

        Modules.get().get(WaypointsModule.class).addDeath(mc.player.getPos());
        mc.player.requestRespawn();
        if (autoRekit.get()) {
            if (chatInfo.get()) info("Rekitting with kit " + kitName.get() + ".");
            ChatUtils.sendPlayerMsg("/kit " + kitName.get());
        }

        event.cancel();
    }
}
