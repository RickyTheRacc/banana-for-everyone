package bananaplus.mixins.meteor;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
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

import java.util.List;
import java.util.Random;


@Mixin(AutoRespawn.class)
public class AutoRespawnMixin extends Module{

    private Setting<Boolean> autoRekit;
    private Setting<Boolean> chatInfo;
    private Setting<String> kitName;
    private boolean shouldRekit;
    private int rekitWait;

    private Setting<Boolean> autoCope;
    private Setting<Integer> copeDelay;
    private Setting<List<String>> messages;
    private boolean shouldCope;
    private int copeWait;




    public AutoRespawnMixin(Category category, String name, String description) {
        super(category, name, description);
    }

    @Inject(method = "<init>", at=@At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        SettingGroup sgGeneral = settings.getDefaultGroup();
        SettingGroup sgCope = settings.createGroup("Auto Cope");

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


        // Auto Cope
        autoCope = sgCope.add(new BoolSetting.Builder()
                .name("auto-cope")
                .description("Automatically make excuses after you die.")
                .defaultValue(false)
                .build()
        );

        copeDelay = sgCope.add(new IntSetting.Builder()
                .name("cope-delay")
                .description("How long to wait in seconds after you die to cope.")
                .defaultValue(1)
                .range(0,5)
                .sliderRange(0,5)
                .visible(autoCope::get)
                .build()
        );

        messages = sgCope.add(new StringListSetting.Builder()
                .name("cope-messages")
                .description("What messages to choose from after you die.")
                .defaultValue(List.of(
                        "Why am I lagging so hard wtf??",
                        "I totem failed that doesn't count",
                        "Leave the green hole challenge",
                        "You're actually so braindead",
                        "How many totems do you have??"
                ))
                .visible(autoCope::get)
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

        if (autoCope.get()) {
            copeWait = copeDelay.get() * 20;
            shouldCope = true;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (rekitWait == 0 && shouldRekit) {
            if (chatInfo.get()) info("Rekitting with kit " + kitName.get() + ".");
            mc.player.sendChatMessage("/kit " + kitName.get());
            shouldRekit = false;
            rekitWait = 60;
        } else if (rekitWait > 0) {
            rekitWait--;
        }

        if (copeWait <= 0 && shouldCope) {
            mc.player.sendChatMessage(getExcuseMessage());
            shouldCope = false;
        } else if (copeWait > 0) {
            copeWait--;
        }
    }

    private String getExcuseMessage() {
        String excuseMessage;

        if (messages.get().isEmpty()) {
            error("Your message list is empty!");
            return "Literally how??";
        } else excuseMessage = messages.get().get(new Random().nextInt(messages.get().size()));

        return excuseMessage;
    }

}
