package bplusdevelopment.modules.misc;

import bplusdevelopment.modules.AddModule;
import bplusdevelopment.utils.Timer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.orbit.EventHandler;

import java.security.MessageDigest;

public class Twerk extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> twerkDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("Twerk Delay")
            .description("In Millis")
            .defaultValue(4)
            .min(2)
            .sliderRange(2,100)
            .build());

    private boolean upp = false;

    public Twerk() {
        super(AddModule.BANANAMINUS, "twerk", "Twerk like the true queen Miley Cyrus");
    }

    private Timer onTwerk = new Timer();

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.options.sneakKey.setPressed(upp);

        if (onTwerk.passedMillis(Double.valueOf(twerkDelay.get()).longValue()) && !upp) {
            onTwerk.reset();
            upp = true;
        }

        if (onTwerk.passedMillis(Double.valueOf(twerkDelay.get()).longValue()) && upp) {
            onTwerk.reset();
            upp = false;
        }

    }

    @Override
    public void onDeactivate() {
        upp = false;
        mc.options.sneakKey.setPressed(false);
        onTwerk.reset();
    }
}
