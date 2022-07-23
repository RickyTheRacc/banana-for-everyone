package bananaplus.modules.misc;

import bananaplus.BananaPlus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.misc.Keybind;

import java.util.List;

public class AutoSexRewrite extends Module {
    public enum TargetMode{
        MiddleClick,
        Keybind,
        Automatic
    }

    public enum DirtyTalkMode {
        Public,
        Private,
        None
    }


    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgDirtyTalk = settings.createGroup("Dirty Talk");
    private final SettingGroup sgHumping = settings.createGroup("Humping");


    // Targeting
    private final Setting<TargetMode> targetMode = sgTargeting.add(new EnumSetting.Builder<TargetMode>()
            .name("target-mode")
            .description("How Auto Sex should target players.")
            .defaultValue(TargetMode.Keybind)
            .build()
    );

    private final Setting<Keybind> keybind = sgTargeting.add(new KeybindSetting.Builder()
            .name("keybind")
            .description("What key to press to start following someone")
            .defaultValue(Keybind.fromKey(-1))
            .visible(() -> targetMode.get() == TargetMode.Keybind)
            .build()
    );

    private final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to select the player to target.")
            .defaultValue(SortPriority.LowestDistance)
            .visible(() -> targetMode.get() == TargetMode.Automatic)
            .build()
    );

    private final Setting<Boolean> ignoreRange = sgTargeting.add(new BoolSetting.Builder()
            .name("ignore-range")
            .description("follow the player even if they are out of range")
            .defaultValue(false)
            .visible(() -> targetMode.get() == TargetMode.Automatic)
            .build()
    );

    private final Setting<Double> followRange = sgTargeting.add(new DoubleSetting.Builder()
            .name("follow-range")
            .description("How close a player must stay to you to be targeted.")
            .defaultValue(25)
            .range(1,50)
            .visible(() -> targetMode.get() == TargetMode.Automatic && !ignoreRange.get())
            .build()
    );



    private final Setting<Boolean> onlyFriends = sgTargeting.add(new BoolSetting.Builder()
            .name("only-friends")
            .description("If you should only have sex with friended players.")
            .defaultValue(true)
            .build()
    );


    // Dirty Talk
    private final Setting<DirtyTalkMode> dirtyTalk = sgDirtyTalk.add(new EnumSetting.Builder<DirtyTalkMode>()
            .name("dirty-talk")
            .description("Send seductive messages to your target! ")
            .defaultValue(DirtyTalkMode.Private)
            .build()
    );

    private final Setting<Double> messageRange = sgDirtyTalk.add(new DoubleSetting.Builder()
            .name("message-range")
            .description("How close to a player you must be to talk dirty to them.")
            .defaultValue(3)
            .range(1,10)
            .visible(() -> dirtyTalk.get() != DirtyTalkMode.None)
            .build()
    );

    private final Setting <String> startMessage = sgDirtyTalk.add(new StringSetting.Builder()
            .name("start-message")
            .description("What Auto Sex should say when you start following a player.")
            .defaultValue("Get over here (enemy), let's have sex!")
            .visible(() -> dirtyTalk.get() != DirtyTalkMode.None)
            .build()
    );

    private final Setting <String> finishMessage = sgDirtyTalk.add(new StringSetting.Builder()
            .name("finish-message")
            .description("What Auto Sex should say when you start following a player.")
            .defaultValue("See you later, (enemy)!")
            .visible(() -> dirtyTalk.get() != DirtyTalkMode.None)
            .build()
    );

    private final Setting<Boolean> randomize = sgDirtyTalk.add(new BoolSetting.Builder()
            .name("randomize")
            .description("Randomize the messages sent for Dirty Talk")
            .defaultValue(false)
            .visible(() -> dirtyTalk.get() != DirtyTalkMode.None)
            .build()
    );

    private final Setting<Integer> delay = sgDirtyTalk.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between messages in ticks.")
            .defaultValue(60)
            .range(40,200)
            .visible(() -> dirtyTalk.get() != DirtyTalkMode.None)
            .build()
    );

    private final Setting<List<String>> sexMessages = sgDirtyTalk.add(new StringListSetting.Builder()
            .name("sex-messages")
            .description("Messages to send to the target during sex.")
            .defaultValue(List.of(
                    "God, I love you so much (enemy)~",
                    "Ahhhh! Fuck me harder (enemy)!",
                    "Please put your cock inside me (enemy)!",
                    "I want to choke on your cock (enemy)!",
                    "Oh god, you're so big (enemy)!",
                    "Treat me like a whore!",
                    "Ahhhhn! Fuck me deeper (enemy)!",
                    "Fill me with your spunk (enemy)~!",
                    "Demolish my bussy (enemy)!~"
                    )
            )
            .visible(() -> dirtyTalk.get() != DirtyTalkMode.None)
            .build()
    );


    // Humping
    private final Setting<Boolean> autoHump = sgHumping.add(new BoolSetting.Builder()
            .name("auto-hump")
            .description("Whether to crouch quickly when close to the target.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyHoles = sgHumping.add(new BoolSetting.Builder()
            .name("only-in-holes")
            .description("Only crouch against the target if you are in a hole.")
            .defaultValue(false)
            .visible(autoHump::get)
            .build()
    );

    private final Setting<Double> humpRange = sgHumping.add(new DoubleSetting.Builder()
            .name("hump-range")
            .description("How close to a player you must be to hump them.")
            .defaultValue(3)
            .range(1,10)
            .visible(autoHump::get)
            .build()
    );

    private final Setting<Double> twerkDelay = sgHumping.add(new DoubleSetting.Builder()
            .name("crouch-delay")
            .description("How fast to crouch against the target in milliseconds.")
            .defaultValue(4)
            .range(2,100)
            .sliderRange(2,100)
            .visible(autoHump::get)
            .build()
    );


    public AutoSexRewrite() {
        super(BananaPlus.MISC, "auto-sex-rewrite", "Have sex with players in the block game!");
    }
}
