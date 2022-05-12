package bananaplus.modules.misc;

import bananaplus.modules.AddModule;
import bananaplus.utils.ReflectionHelper;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;

//oh this also ares forgor
public class ReloadSoundSystem extends Module {

    public ReloadSoundSystem() {
        super(AddModule.MISC, "reload-sounds", "Reloads Minecraft's sound system");
    }

    @Override
    public void onActivate() {
        SoundSystem soundSystem = ReflectionHelper.getPrivateValue(SoundManager.class, mc.getSoundManager(), "soundSystem", "field_5590");
        soundSystem.reloadSounds();
        toggle();
    }
}