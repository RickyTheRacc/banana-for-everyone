package me.ricky.bananaplus.mixin.meteor;

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

import java.util.List;


@Mixin(value = AutoRespawn.class, remap = false)
public abstract class AutoRespawnMixin extends Module{
    @Unique private Setting<Boolean> autoSend;
    @Unique private Setting<List<String>> messages;

    public AutoRespawnMixin() {
        super(Categories.Player, "auto-respawn", "Automatically respawns after death.");
    }

    @Inject(method = "<init>", at=@At("TAIL"))
    private void onInit(CallbackInfo ci) {
        SettingGroup sgGeneral = settings.getDefaultGroup();

        // General

        autoSend = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-send")
            .description("Send messages after you die.")
            .defaultValue(false)
            .build()
        );

        messages = sgGeneral.add(new StringListSetting.Builder()
            .name("messages")
            .description("The messages to send after you die.")
            .visible(autoSend::get)
            .build()
        );
    }

    @Inject(method = "onOpenScreenEvent", at = @At("TAIL"))
    private void sendRekitMessage(OpenScreenEvent event, CallbackInfo ci) {
        if (autoSend.get()) messages.get().forEach(ChatUtils::sendPlayerMsg);
    }
}