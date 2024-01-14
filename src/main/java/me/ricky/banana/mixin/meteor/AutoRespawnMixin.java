package me.ricky.banana.mixin.meteor;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.misc.AutoRespawn;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = AutoRespawn.class, remap = false)
public abstract class AutoRespawnMixin extends Module{
    @Unique private Setting<Boolean> autoRekit;
    @Unique private Setting<Boolean> chatInfo;
    @Unique private Setting<String> kitName;

    public AutoRespawnMixin() {
        super(Categories.Player, "auto-respawn", "Automatically respawns after death.");
    }

    @Inject(method = "<init>", at=@At("TAIL"))
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

    @Inject(method = "onOpenScreenEvent", at = @At("TAIL"))
    private void sendRekitMessage(OpenScreenEvent event, CallbackInfo ci) {
        if (autoRekit.get()) {
            if (chatInfo.get()) info("Rekitting with kit " + kitName.get() + ".");
            ChatUtils.sendPlayerMsg("/kit " + kitName.get());
        }
    }
}