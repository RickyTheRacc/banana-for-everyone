package bananaplus.modules.misc;

import bananaplus.modules.BananaPlus;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

public class AutoFollow extends Module {

    public AutoFollow() {
        super(BananaPlus.MISC, "auto-Follow", "Follow another player in different ways");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum Mode {MiddleClickToFollow, FollowPlayer, BindClickFollow}

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("Mode").description("The mode at which to follow the player.").defaultValue(Mode.BindClickFollow).build());
    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder().name("follow-keybind").description("What key to press to start following someone").defaultValue(Keybind.fromKey(-1)).visible(() -> mode.get() == Mode.BindClickFollow).build());

    private final Setting<Boolean> onlyFriend = sgGeneral.add(new BoolSetting.Builder().name("only-follow-friends").description("Whether or not to only follow friends.").defaultValue(false).build());
    private final Setting<Boolean> onlyOther = sgGeneral.add(new BoolSetting.Builder().name("don't-follow-friends").description("Whether or not to follow friends.").defaultValue(false).visible(() -> mode.get() != Mode.FollowPlayer).build());
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("Range").description("The range in which it follows a random player").defaultValue(20).min(0).sliderMax(200).visible(() -> mode.get() == Mode.FollowPlayer).build());
    private final Setting<Boolean> ignoreRange = sgGeneral.add(new BoolSetting.Builder().name("keep-Following").description("follow the player even if they are out of range").defaultValue(false).visible(() -> mode.get() == Mode.FollowPlayer).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("How to select the player to target.").defaultValue(SortPriority.LowestDistance).visible(() -> mode.get() == Mode.FollowPlayer).build());
    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder().name("message").description("Sends a message to the player when you start/stop following them.").defaultValue(false).build());
    private final Setting<Boolean> dm = sgGeneral.add(new BoolSetting.Builder().name("private-msg").description("sends a private chat msg to the person").defaultValue(false).visible(message::get).build());
    private final Setting<Boolean> pm = sgGeneral.add(new BoolSetting.Builder().name("public-msg").description("sends a public chat msg").defaultValue(false).visible(message::get).build());


    private int messageI, timer;


    @Override
    public void onActivate() {
        messageI = 0;
    }

    boolean isFollowing = false;
    String playerName;
    Entity playerEntity;
    float dis = 1.5f;


    int iPublic;
    boolean pressed = false;
    boolean alternate = true;

    //middle click mode
    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if(mode.get() == Mode.MiddleClickToFollow){
            if (event.action == KeyAction.Press && event.button == GLFW_MOUSE_BUTTON_MIDDLE && mc.currentScreen == null && mc.targetedEntity != null && mc.targetedEntity instanceof PlayerEntity) {
                if (!isFollowing) {

                    if (!Friends.get().isFriend((PlayerEntity) mc.targetedEntity) && onlyFriend.get()) return;
                    if (Friends.get().isFriend((PlayerEntity) mc.targetedEntity) && onlyOther.get()) return;

                    mc.player.sendChatMessage(Config.get().prefix.get() + "baritone follow player " + mc.targetedEntity.getEntityName());

                    playerName = mc.targetedEntity.getEntityName();
                    playerEntity = mc.targetedEntity;

                    if (message.get()) {
                        startMsg();
                    }

                    isFollowing = true;
                } else {
                    mc.player.sendChatMessage(Config.get().prefix.get() + "baritone stop");

                    if (message.get()) {
                        endMsg();
                    }

                    playerName = null;
                    isFollowing = false;
                }
            }
            else if(event.action == KeyAction.Press && event.button == GLFW_MOUSE_BUTTON_MIDDLE && isFollowing){
                mc.player.sendChatMessage(Config.get().prefix.get() + "baritone stop");

                if (message.get()) {
                    endMsg();
                }
                playerName = null;
                isFollowing = false;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onTick(TickEvent.Post event) {

        if(mode.get() == Mode.BindClickFollow && keybind != null)
        {
            if(keybind.get().isPressed() && !pressed && !alternate)
            {
                if (isFollowing)
                {
                    mc.player.sendChatMessage(Config.get().prefix.get() + "baritone stop");

                    if (message.get()) {
                        endMsg();
                    }

                    pressed = true;
                    alternate = true;
                    playerName = null;
                    isFollowing = false;
                }
            }

            if (!keybind.get().isPressed())
            {
                pressed = false;
            }

            if(keybind.get().isPressed() && !pressed && alternate && mc.currentScreen == null && mc.targetedEntity != null && mc.targetedEntity instanceof PlayerEntity)
            {
                if (!isFollowing) {

                    if (!Friends.get().isFriend((PlayerEntity) mc.targetedEntity) && onlyFriend.get()) return;
                    if (Friends.get().isFriend((PlayerEntity) mc.targetedEntity) && onlyOther.get()) return;

                    mc.player.sendChatMessage(Config.get().prefix.get() + "baritone follow player " + mc.targetedEntity.getEntityName());

                    playerName = mc.targetedEntity.getEntityName();
                    playerEntity = mc.targetedEntity;

                    if (message.get()) {
                        startMsg();
                    }

                    pressed = true;
                    alternate = false;
                    isFollowing = true;
                }
            }
        }

        if(mode.get() == Mode.FollowPlayer){

            if (!isFollowing) {
                playerEntity = TargetUtils.getPlayerTarget(range.get(), priority.get());
                if (playerEntity == null) return;
                playerName = playerEntity.getEntityName();

                if (!Friends.get().isFriend((PlayerEntity) playerEntity) && onlyFriend.get()) return;

                mc.player.sendChatMessage(Config.get().prefix.get() + "baritone follow player " + playerName);

                if (message.get()) {
                    startMsg();
                }

                isFollowing = true;
            }

            if (!playerEntity.isAlive() || (playerEntity.distanceTo(mc.player) > range.get() && !ignoreRange.get())) {
                if (message.get()) {
                    endMsg();
                }

                mc.player.sendChatMessage(Config.get().prefix.get() + "baritone stop");
                playerEntity = null;
                playerName = null;
                isFollowing = false;
            }
        }

    }

    @Override
    public void onDeactivate() {

        mc.player.sendChatMessage(Config.get().prefix.get() + "baritone stop");
        playerEntity = null;
        playerName = null;
        isFollowing = false;
    }

    public void startMsg()
    {
        if (dm.get()) {
            mc.player.sendChatMessage("/msg " + playerName + " I am now following you using Banana+");
        }

        if (pm.get()) {
            mc.player.sendChatMessage("I am now following " + playerName + " using Banana+");
        }
    }


    public void endMsg()
    {
        if (dm.get()) {
            mc.player.sendChatMessage("/msg " + playerName + " I am no longer following you");
        }

        if (pm.get()) {
            mc.player.sendChatMessage("I am no longer following " + playerName);
        }
    }
}