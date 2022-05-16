package bananaplus.modules.misc;

import bananaplus.modules.BananaPlus;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.NarratorMode;
import org.lwjgl.glfw.GLFW;

public class AntiNarrator extends Module {
    public enum Mode {
        Cancel,
        Disable
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    // General
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .defaultValue(Mode.Disable)
            .build()
    );

    private final Setting<Boolean> macOs = sgGeneral.add(new BoolSetting.Builder()
            .name("apple-PC")
            .description("Lol imagine being on an apple PC weirdo")
            .defaultValue(false)
            .visible(() -> mode.get() == Mode.Cancel)
            .build()
    );


    public AntiNarrator() {
        super(BananaPlus.MISC, "anti-narrator", "Stops the annoying narrator from popping up. Cancel = Cancel event when you press Ctrl+B, Disable = Automatically disables narrator when you turn it on.");
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) return;
        if (!mc.options.narrator.equals(NarratorMode.OFF) && mode.get() == Mode.Disable) mc.options.narrator = NarratorMode.OFF;
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (!isActive()) return;
        if (mode.get() == Mode.Cancel) {
            if (Input.isKeyPressed(GLFW.GLFW_KEY_B) && (macOs.get() ? Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_SUPER) : Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL))) event.cancel();
        }
    }
}