package bananaplus.utils;

import com.sun.jna.Platform;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

public class AntiNarrator {
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(AntiNarrator.class);
    }

    public enum OS {
        PC,
        Mac,
        Other
    }

    private static OS getOS() {
        if (Platform.isWindows()) return OS.PC;
        if (Platform.isMac()) return OS.Mac;
        return OS.Other;
    }

    // Cancel the keybind to enable narrator
    @EventHandler
    private static void onKey(KeyEvent event) {
        if ((Input.isKeyPressed(GLFW.GLFW_KEY_B) && Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_SUPER) && getOS() == OS.Mac)) event.cancel();
        else if ((Input.isKeyPressed(GLFW.GLFW_KEY_B) && Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL))) event.cancel();
    }
}

// I left out disable mode because there is a small chance that may actually use narrator
// This way the only way to turn it on is to use the setting and do it intentionally