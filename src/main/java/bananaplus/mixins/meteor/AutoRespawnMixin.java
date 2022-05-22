package bananaplus.mixins.meteor;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoRespawn;
import meteordevelopment.meteorclient.systems.modules.render.WaypointsModule;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AutoRespawn.class)
public class AutoRespawnMixin extends Module{

    private SettingGroup sgGeneral;

    private Setting<Boolean> autoRekit;
    private Setting<Boolean> chatInfo;
    private Setting<String> kitName;

    private boolean shouldRekit = false;
    private int rekitWait = 60;

    public AutoRespawnMixin(Category category, String name, String description) {
        super(category, name, description);
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
                .defaultValue(true)
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

    @EventHandler(priority = EventPriority.HIGH)
    private void onOpenScreenEvent(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;

        Modules.get().get(WaypointsModule.class).addDeath(mc.player.getPos());
        mc.player.requestRespawn();
        event.cancel();

        if (autoRekit.get()) shouldRekit = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (rekitWait == 0 && shouldRekit) {
            if (chatInfo.get()) info("Rekitting with kit " + kitName.get() + ".");
            mc.player.sendChatMessage("/kit " + kitName.get());
            shouldRekit = false;
            rekitWait = 60;
        }

        else if (rekitWait > 0) {
            rekitWait--;
        }
    }
}
