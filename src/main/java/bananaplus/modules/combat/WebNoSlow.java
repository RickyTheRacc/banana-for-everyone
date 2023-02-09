package bananaplus.modules.combat;

import bananaplus.BananaPlus;
import bananaplus.utils.BEntityUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;

public class WebNoSlow extends Module {
    public enum WebMode {
        Vanilla,
        Timer,
        Adaptive,
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    public final Setting<WebMode> web = sgGeneral.add(new EnumSetting.Builder<WebMode>()
            .name("web")
            .description("Whether or not cobwebs will not slow you down.")
            .defaultValue(WebMode.Adaptive)
            .build()
    );

    public final Setting<Integer> webTimer = sgGeneral.add(new IntSetting.Builder()
            .name("web-timer")
            .description("The timer value for WebMode Timer.")
            .defaultValue(10)
            .min(1)
            .sliderMin(1)
            .visible(() -> web.get() != WebMode.Vanilla)
            .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Notifies you in chat when you are webbed.")
            .defaultValue(false)
            .build()
    );


    public WebNoSlow() {
        super(BananaPlus.COMBAT, "web-no-slow", "An improved no-slow for webs.");
    }


    private boolean resetTimer;
    private boolean sentMessage;


    @Override
    public void onActivate() {
        resetTimer = false;
        sentMessage = false;
    }

    public boolean ignoreWebs() {
        if (web.get() == WebMode.Vanilla) return true;
        return (web.get() == WebMode.Adaptive && mc.player.isOnGround());
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (BEntityUtils.isWebbed(mc.player)) {
            if (chatInfo.get() && !sentMessage) error("You are webbed!");
            sentMessage = true;
        } else sentMessage = false;

        if (web.get() != WebMode.Vanilla) {
            if (BEntityUtils.isWebbed(mc.player) && !mc.player.isOnGround()) {
                resetTimer = false;
                Modules.get().get(Timer.class).setOverride(webTimer.get());
            } else if (!resetTimer) {
                Modules.get().get(Timer.class).setOverride(Timer.OFF);
                resetTimer = true;
            }
        }
    }
}
