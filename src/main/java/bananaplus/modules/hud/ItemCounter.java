package bananaplus.modules.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.*;

import java.util.*;

public class ItemCounter extends HudElement {
    public enum SortMode {
        Longest,
        Shortest
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    // General
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
            .name("sort-mode")
            .description("How to sort the binds list.")
            .defaultValue(SortMode.Shortest)
            .build()
    );

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("items")
            .description("Which items to display in the counter list.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );


    public ItemCounter(HUD hud) {
        super(hud, "item-counter", "Display the amount of selected items in your inventory.", false);
    }


    private final ArrayList<String> itemCounter = new ArrayList<>();
    private final HashMap<Item, Integer> itemCounts = new HashMap<Item, Integer>();

    @Override
    public void update(HudRenderer renderer) {
        if (!Utils.canUpdate()) return;

        updateCounter();

        double width = 0;
        double height = 0;
        int i = 0;

        if (itemCounter.isEmpty()) {
            String t = "Item Counter";
            width = Math.max(width, renderer.textWidth(t));
            height += renderer.textHeight();
        } else {
            for (String counter : itemCounter) {
                width = Math.max(width, renderer.textWidth(counter));
                height += renderer.textHeight();
                if (i > 0) height += 2;
                i++;
            }
        }
        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (!Utils.canUpdate()) return;

        updateCounter();

        double x = box.getX();
        double y = box.getY();
        int i = 0;

        if (itemCounter.isEmpty()) {
            String t = "Item Counter";
            renderer.text(t, x + box.alignX(renderer.textWidth(t)), y, hud.secondaryColor.get());
        } else {
            for (String counter: itemCounter) {
                renderer.text(counter, x + box.alignX(renderer.textWidth(counter)), y, hud.secondaryColor.get());
                y += renderer.textHeight();
                if (i > 0) y += 2;
                i++;
            }
        }
    }


    private void updateCounter() {
        items.get().sort(Comparator.comparingDouble(value -> getName(value).length()));

        itemCounter.clear();
        for (Item item: items.get()) itemCounter.add(getName(item) + ": " + InvUtils.find(item).count());

        if (sortMode.get().equals(SortMode.Shortest)) {
            itemCounter.sort(Comparator.comparing(String::length));
        } else {
            itemCounter.sort(Comparator.comparing(String::length).reversed());
        }
    }

    public static String getName(Item item) {
        if (item instanceof BedItem) return "Fuck You!!!!!";
        if (item instanceof ExperienceBottleItem) return "XP Bottles";
        if (item instanceof EndCrystalItem) return "Crystals";
        if (item instanceof EnchantedGoldenAppleItem) return "Gapples";
        if (item instanceof EnderPearlItem) return "Pearls";
        if (item == Items.TOTEM_OF_UNDYING) return "Totems";
        if (item == Items.ENDER_CHEST) return "Echests";
        return Names.get(item);
    }
}