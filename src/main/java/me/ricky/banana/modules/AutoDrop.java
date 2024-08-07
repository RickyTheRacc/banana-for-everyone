package me.ricky.banana.modules;

import me.ricky.banana.BananaPlus;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.render.PeekScreen;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("ConstantConditions")
public class AutoDrop extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> junkItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("junk-items")
        .description("Which items to treat as junk.")
        .defaultValue(
            Items.TUFF,
            Items.DEEPSLATE,
            Items.PUFFERFISH,
            Items.SALMON,
            Items.COD,
            Items.LEATHER,
            Items.GLASS_BOTTLE
        )
        .build()
    );

    private final Setting<Boolean> invClear = sgGeneral.add(new BoolSetting.Builder()
        .name("inv-clearing")
        .description("Throw out items from your inventory.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chestClear = sgGeneral.add(new BoolSetting.Builder()
        .name("chest-clearing")
        .description("Throw out items from chests.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyOnKey = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-key")
        .description("Only throw items out when a key is pressed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> clearKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("clear-key")
        .description("The key to press to clear items from your inventory.")
        .visible(onlyOnKey::get)
        .build()
    );

    public AutoDrop() {
        super(BananaPlus.CATEGORY, "auto-drop", "Automatically drop items from your inventory and chests.");
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (onlyOnKey.get()) return;
        dropItems();
    }

    @EventHandler
    private void onKeyPressed(KeyEvent event) {
        if (!onlyOnKey.get() || event.action != KeyAction.Release) return;
        if (!clearKey.get().matches(true, event.key, 0)) return;

        dropItems();
    }

    @EventHandler
    private void onMouseButtonPressed(MouseButtonEvent event) {
        if (!onlyOnKey.get() || event.action != KeyAction.Release) return;
        if (!clearKey.get().matches(false, event.button, 0)) return;

        dropItems();
    }

    private void dropItems() {
        if (!Utils.canUpdate()) return;
        Set<Integer> slotIds = new HashSet<>();

        if (invClear.get() && mc.player.currentScreenHandler instanceof PlayerScreenHandler handler) {
            handler.slots.forEach(slot -> {
                Item item = slot.getStack().getItem();

                if (item == Items.AIR) return;
                if (!junkItems.get().contains(item)) return;

                slotIds.add(slot.id);
            });
        }

        if (chestClear.get() && mc.currentScreen instanceof HandledScreen<?> screen) {
            if (screen instanceof PeekScreen || screen instanceof InventoryScreen) return;

            screen.getScreenHandler().slots.forEach(slot -> {
                Item item = slot.getStack().getItem();

                if (item == Items.AIR) return;
                if (!junkItems.get().contains(item)) return;

                slotIds.add(slot.id);
            });
        }

        if (slotIds.isEmpty()) return;

        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        float oppositeYaw = (mc.player.getYaw(tickDelta) + 180.0F) % 360.0F;
        if (oppositeYaw > 180) oppositeYaw -= 360.0F;

        Rotations.rotate(oppositeYaw, mc.player.getPitch(tickDelta), () -> {
            slotIds.forEach(id -> InvUtils.drop().slotId(id));
        });
    }
}
