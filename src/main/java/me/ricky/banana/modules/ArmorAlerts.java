package me.ricky.banana.modules;

import me.ricky.banana.BananaPlus;
import me.ricky.banana.systems.BananaModule;
import me.ricky.banana.utils.ServerUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.sound.SoundEvents;

import java.util.HashSet;
import java.util.Set;

public class ArmorAlerts extends BananaModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
        .name("threshold")
        .description("Notify you when your armor reaches this durability.")
        .defaultValue(20)
        .range(1,99)
        .sliderRange(1,99)
        .build()
    );

    private final Setting<Boolean> notifySelf = sgGeneral.add(new BoolSetting.Builder()
        .name("notify-self")
        .description("Send warnings about your own armor.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> notifyEnemies = sgGeneral.add(new BoolSetting.Builder()
        .name("notify-enemies")
        .description("Send warnings about your enemy's armor.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> playSound = sgGeneral.add(new BoolSetting.Builder()
        .name("play-sound")
        .description("Plays a ding sound to alert you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> volume = sgGeneral.add(new IntSetting.Builder()
        .name("volume")
        .description("The volume of the sound played.")
        .defaultValue(50)
        .min(1)
        .sliderRange(1,100)
        .visible(playSound::get)
        .build()
    );

    public ArmorAlerts() {
        super(BananaPlus.CATEGORY, "armor-alerts", "Notifies you when your armor is low.");
    }

    private final Set<PlayerEntity> helmets = new HashSet<>();
    private final Set<PlayerEntity> chestplates = new HashSet<>();
    private final Set<PlayerEntity> leggings = new HashSet<>();
    private final Set<PlayerEntity> boots = new HashSet<>();

    private boolean shouldPlaySound = false;

    @Override
    public void onActivate() {
        shouldPlaySound = false;
        helmets.clear();
        chestplates.clear();
        leggings.clear();
        boots.clear();
    }

    @EventHandler
    public void onPostTick(TickEvent.Post event) {
        shouldPlaySound = false;

        for (PlayerEntity player: ServerUtil.getPlayers()) {
            if (Friends.get().isFriend(player)) continue;
            if (!notifySelf.get() && player == mc.player) continue;
            if (!notifyEnemies.get() && !Friends.get().isFriend(player)) continue;

            for (int i = 0; i < 4; i++) {
                ItemStack stack = player.getInventory().getArmorStack(i);
                boolean repaired = isRepaired(stack);

                Set<PlayerEntity> armorList = switch(i) {
                    case 0 -> helmets;
                    case 1 -> chestplates;
                    case 2 -> leggings;
                    case 3 -> boots;
                    default -> throw new IllegalStateException(STR."Unexpected value: \{i}");
                };

                if (repaired) armorList.remove(player);
                if (!armorList.contains(player) && !repaired) {
                    armorList.add(player);
                    notify(stack.getItem(), player);
                    shouldPlaySound = true;
                }
            }
        }

        if (shouldPlaySound && playSound.get()) mc.getSoundManager().play(PositionedSoundInstance.master(
            SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.2f, volume.get()
        ));
    }

    private boolean isRepaired(ItemStack stack) {
        if (stack.getMaxDamage() <= 0 || EnchantmentHelper.getLevel(Enchantments.MENDING, stack) == 0) return true;
        return ((double) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()) * 100 > threshold.get();
    }

    private void notify(Item item, PlayerEntity player) {
        String playerName = (player == mc.player) ? "Your " : STR."\{player.getName().getString()}'s ";
        String itemName = item.getName().getString().toLowerCase();

        if ((item instanceof ArmorItem && item != Items.TURTLE_HELMET) || item instanceof ToolItem) {
            String[] nameArray = itemName.split(" ");
            itemName = nameArray[nameArray.length - 1];
        }

        String infoText = STR."\{playerName}\{itemName}\{(itemName.endsWith("s")) ? " are " : " is "}low!";
        warning(infoText);
    }
}
