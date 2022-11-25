package bananaplus.modules.misc;

import bananaplus.BananaPlus;
import bananaplus.utils.BPlayerUtils;
import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.StoppedUsingItemEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

public class BindClickExtra extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    // General
    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
            .name("keybind")
            .description("What key to press to start use an item.")
            .defaultValue(Keybind.fromKey(GLFW_MOUSE_BUTTON_MIDDLE))
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("item-mode")
            .description("Which item to use when you press the button.")
            .defaultValue(Mode.Pearl)
            .build()
    );

    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder()
            .name("message")
            .description("Message players when you add them as a friend..")
            .defaultValue(false)
            .visible(() -> mode.get() == Mode.Friend)
            .build()
    );

    private final Setting<SwitchMode> switchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
            .name("switch-mode")
            .description("Which item to use when you press the button.")
            .defaultValue(SwitchMode.Inventory)
            .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Alert you if you don't have the specified item.")
            .defaultValue(false)
            .build()
    );


    public BindClickExtra() {
        super(BananaPlus.MISC, "bind-extra", "Use items from your inventory with a button.");
    }


    private boolean pressed = false;
    private boolean isUsing = false;
    private FindItemResult result;

    @Override
    public void onActivate() {
        pressed = false;
    }

    @Override
    public void onDeactivate() {
        stopIfUsing();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.currentScreen != null) return;

        if (!keybind.get().isPressed()) pressed = false;

        if (keybind.get().isPressed() && !pressed) {
            if (mode.get() == Mode.Friend) {
                if (mc.targetedEntity == null || !(mc.targetedEntity instanceof PlayerEntity player)) return;

                if (!Friends.get().isFriend(player)) {
                    Friends.get().add(new Friend(player));
                    if (message.get()) BPlayerUtils.sendDM(player.getEntityName(), "I just added you as a friend.");
                } else Friends.get().remove(Friends.get().get(player));
            } else {
                result = InvUtils.find(mode.get().item);

                if (!result.found() || !(switchMode.get() == SwitchMode.Inventory && !result.isHotbar())) {
                    if (chatInfo.get()) error("Couldn't find the selected item!");
                    return;
                }

                if (switchMode.get() == SwitchMode.Inventory) {
                    InvUtils.move().from(result.slot()).to(mc.player.getInventory().selectedSlot);
                } else InvUtils.swap(result.slot(), true);

                pressed = true;

                switch (mode.get().type) {
                    case Single -> {
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        swapBack();
                    }

                    case Longer -> {
                        mc.options.useKey.setPressed(true);
                        isUsing = true;
                    }
                }

                if (isUsing) {
                    boolean pressed = true;

                    if (mc.player.getMainHandStack().getItem() instanceof BowItem) {
                        pressed = BowItem.getPullProgress(mc.player.getItemUseTime()) < 1;
                    }

                    mc.options.useKey.setPressed(pressed);
                }

                if (isUsing) pressed = true;
            }
        }
    }

    @EventHandler
    private void onFinishUsingItem(FinishUsingItemEvent event) {
        stopIfUsing();
    }

    @EventHandler
    private void onStoppedUsingItem(StoppedUsingItemEvent event) {
        stopIfUsing();
    }

    private void stopIfUsing() {
        if (isUsing) {
            mc.options.useKey.setPressed(false);
            swapBack();
            isUsing = false;
        }
    }

    private void swapBack() {
        if (switchMode.get() == SwitchMode.Inventory) {
            InvUtils.move().from(result.slot()).to(mc.player.getInventory().selectedSlot);
        } else InvUtils.swapBack();
    }

    public enum Mode {
        Pearl (Items.ENDER_PEARL, Type.Single),
        Gapple (Items.ENCHANTED_GOLDEN_APPLE, Type.Longer),
        Rocket (Items.FIREWORK_ROCKET, Type.Single),
        Chorus (Items.CHORUS_FRUIT, Type.Longer),
        Friend (null, null);

        private final Item item;
        private final Type type;

        Mode(Item item, Type type) {
            this.item = item;
            this.type = type;
        }
    }

    public enum SwitchMode {
        Silent,
        Inventory
    }

    public enum Type {
        Single,
        Longer
    }
}
