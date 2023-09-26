package bananaplus.mixins.meteor;

import bananaplus.system.BananaConfig;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(value = Systems.class, remap = false)
public abstract class SystemsMixin {
    @Final @Shadow private static Map<Class<? extends System>, System<?>> systems;
    @Shadow private static System<?> add(System<?> system) {
        systems.put(system.getClass(), system);
        MeteorClient.EVENT_BUS.subscribe(system);
        system.init();
        return system;
    }

    /**
     * @author RickyTheRacc
     * @reason Add BananaConfig tab
     */
    @Overwrite
    public static void init() {
        Config meteorConfig = new Config();
        System<?> meteorSystem = add(meteorConfig);
        meteorSystem.init();
        meteorSystem.load();

        BananaConfig bananaConfig = new BananaConfig();
        System<?> bananaSystem = add(bananaConfig);
        bananaSystem.init();
        bananaSystem.load();

        // Registers the colors from config tabs. This allows rainbow colours to work for friends.
        meteorConfig.settings.registerColorSettings(null);
        bananaConfig.settings.registerColorSettings(null);

        add(new Modules());
        add(new Macros());
        add(new Friends());
        add(new Accounts());
        add(new Waypoints());
        add(new Profiles());
        add(new Proxies());
        add(new Hud());

        MeteorClient.EVENT_BUS.subscribe(Systems.class);
    }
}
