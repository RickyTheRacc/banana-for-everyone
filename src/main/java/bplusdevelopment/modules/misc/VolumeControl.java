package bplusdevelopment.modules.misc;

import bplusdevelopment.modules.AddModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.sound.SoundCategory;

public class VolumeControl extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgQuickControl = settings.createGroup("Quick Control");

    // General

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> instaMute = sgGeneral.add(new BoolSetting.Builder()
            .name("mute-on-activate")
            .description("Will mute your minecraft sounds on activation.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> reconfigure = sgGeneral.add(new BoolSetting.Builder()
            .name("reconfigure")
            .description("Will put your minecraft volume back to the level it was on when the module is disabled.")
            .defaultValue(true)
            .visible(instaMute::get)
            .build());

    // Quick control

    private final Setting<Double> increment = sgQuickControl.add(new DoubleSetting.Builder()
            .name("increment")
            .description("How much the volume should be changed by everytime for the quick control.")
            .defaultValue(5)
            .range(1, 100)
            .sliderRange(1, 100)
            .build());

    private final Setting<Keybind> increaseBind = sgQuickControl.add(new KeybindSetting.Builder()
            .name("increase-keybind")
            .description("What key to press in order to increase your volume.")
            .defaultValue(Keybind.none())
            .build());

    private final Setting<Keybind> decreaseBind = sgQuickControl.add(new KeybindSetting.Builder()
            .name("decrease-keybind")
            .description("What key to press in order to decrease your volume.")
            .defaultValue(Keybind.none())
            .build());

    public VolumeControl() {
        super(AddModule.BANANAMINUS, "volume-control", "Allows you to control your volume easier.");
    }

    private float initVolume;

    @Override
    public void onActivate() {
        initVolume = mc.options.getSoundVolume(SoundCategory.MASTER);
        if (instaMute.get()) {
            mc.options.setSoundVolume(SoundCategory.MASTER, 0);
            if (debug.get()) warning("Minecraft is now muted!");
        }
    }

    @Override
    public void onDeactivate() {
        if (instaMute.get() && reconfigure.get()) {
            mc.options.setSoundVolume(SoundCategory.MASTER, initVolume);
            if (debug.get()) warning("Setting volume back to " + Math.round(initVolume * 100) + "%%");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        float floatVolume = mc.options.getSoundVolume(SoundCategory.MASTER);

        if (increaseBind.get().isPressed()) {
            mc.options.setSoundVolume(SoundCategory.MASTER, floatVolume + volumeTransformer());
            if (debug.get()) warning("Volume now at " + Math.round((floatVolume + volumeTransformer()) * 100) + "%%");
        }

        if (decreaseBind.get().isPressed()) {
            mc.options.setSoundVolume(SoundCategory.MASTER, floatVolume - volumeTransformer());
            if (debug.get()) warning("Volume now at " + Math.round((floatVolume - volumeTransformer()) * 100) + "%%");
        }
    }

    private float volumeTransformer() {
        return increment.get().floatValue() / 100;
    }
}