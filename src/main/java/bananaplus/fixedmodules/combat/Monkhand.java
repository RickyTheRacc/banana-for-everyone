package bananaplus.fixedmodules.combat;

import bananaplus.BananaPlus;
import bananaplus.enums.BlockType;
import bananaplus.fixedutils.CombatUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.BedAura;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

import java.util.List;
import java.util.function.Predicate;

import static meteordevelopment.orbit.EventPriority.HIGHEST;

public class Monkhand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTotems = settings.createGroup("Totems");
    private final SettingGroup sgGapples = settings.createGroup("Gapples");
    private final SettingGroup sgCombat = settings.createGroup("Combat");

    // General

    private final Setting<Integer> swapDelay = sgGeneral.add(new IntSetting.Builder()
        .name("swap-delay")
        .description("How many ticks to wait between movements.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0,20)
        .build()
    );

    private final Setting<Boolean> useHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("use-hotbar")
        .description("Whether to use items from your hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> usePause = sgGeneral.add(new BoolSetting.Builder()
        .name("use-pause")
        .description("Won't switch while you're using items in your mainhand.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> spoofScreen = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-screen")
        .description("Spoof closing your inventory after each movement, can trick some stricter ACs.")
        .defaultValue(true)
        .build()
    );

    // Totems

    private final Setting<TotemMode> totemMode = sgTotems.add(new EnumSetting.Builder<TotemMode>()
        .name("autototem-mode")
        .description("Strict will always hold a totem.")
        .defaultValue(TotemMode.Smart)
        .build()
    );

    private final Setting<Double> minHealth = sgTotems.add(new DoubleSetting.Builder()
        .name("min-health")
        .description("How low you have to be to hold a totem.")
        .defaultValue(10)
        .range(0,36)
        .sliderRange(0,36)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Double> armorOffset = sgTotems.add(new DoubleSetting.Builder()
        .name("armor-offset")
        .description("Raise your min health by this amount for each piece of armor you're missing.")
        .defaultValue(2)
        .range(0,36)
        .sliderRange(0,36)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Double> holeOffset = sgTotems.add(new DoubleSetting.Builder()
        .name("hole-offset")
        .description("Lower your min health by this amount if you're in a safe hole.")
        .defaultValue(4)
        .range(0,36)
        .sliderRange(0,36)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> elytraCheck = sgTotems.add(new BoolSetting.Builder()
        .name("elytra-check")
        .description("Always hold a totem while using elytra.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> fallingCheck = sgTotems.add(new BoolSetting.Builder()
        .name("falling-check")
        .description("Hold a totem if fall damage could kill you.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> damageCheck = sgTotems.add(new BoolSetting.Builder()
        .name("damage-check")
        .description("Hold a totem if you could take fatal damage next tick.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    // Gapples

    private final Setting<Boolean> rightClickGap = sgGapples.add(new BoolSetting.Builder()
        .name("right-click")
        .description("Only hold gapples while holding your use key.")
        .defaultValue(true)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> alwaysOnWeapon = sgGapples.add(new BoolSetting.Builder()
        .name("always-on-weapon")
        .description("Always hold gapples while holding a weapon.")
        .defaultValue(true)
        .visible (() -> totemMode.get() != TotemMode.Strict && rightClickGap.get())
        .build()
    );

    private final Setting<Boolean> preferCrapples = sgGapples.add(new BoolSetting.Builder()
        .name("prefer-crapples")
        .description("Prefer holding regular gapples over egaps.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> onlyInHoles = sgGapples.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Only allow you to gap if you're in a hole.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    // Combat

    private Setting<List<Module>> crystalAura = sgCombat.add(new ModuleListSetting.Builder()
        .name("crystal-auras")
        .description("Hold beds if any of these are active.")
        .defaultValue(CrystalAura.class)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private Setting<List<Module>> bedAuras = sgCombat.add(new ModuleListSetting.Builder()
        .name("bed-auras")
        .description("Hold beds if any of these are active.")
        .defaultValue(BedAura.class)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> whileMining = sgCombat.add(new BoolSetting.Builder()
        .name("while-mining")
        .description("Always hold crystals while mining a block.")
        .defaultValue(false)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    private final Setting<Boolean> enemiesNearby = sgCombat.add(new BoolSetting.Builder()
        .name("near-enemies")
        .description("Always hold crystals if you're near players you don't have added.")
        .defaultValue(true)
        .visible (() -> totemMode.get() != TotemMode.Strict)
        .build()
    );

    public Monkhand() {
        super(BananaPlus.FIXED, "monkhand", "The best offhand in the game.");
    }

    private double delay;
    private Item toHold;
    private Item mainItem;
    private Item offItem;
    private FindItemResult result;

    @Override
    public void onActivate() {
        delay = 0.0;
        setItemNextTick();
    }

    @EventHandler(priority = HIGHEST)
    private void onPreTick(TickEvent.Pre event) {
        delay -= TickRate.INSTANCE.getTickRate() / 20.0;
        if (delay > 0) return;

        if (mc.currentScreen instanceof GenericContainerScreen) return;
        if (usePause.get() && mc.player.isUsingItem()) return;

        offItem = mc.player.getOffHandStack().getItem();

        if (offItem != toHold) {

        }

        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        mainItem = mc.player.getMainHandStack().getItem();
        offItem = mc.player.getOffHandStack().getItem();
        setItemNextTick();
    }

    private void setItemNextTick() {
        findItem(Items.TOTEM_OF_UNDYING);

        if (result.found()) {
            if (totemMode.get() == TotemMode.Strict) return;
            if (elytraCheck.get() && isWearingElytra()) return;

            double playerHealth = PlayerUtils.getTotalHealth();
            if (CombatUtil.isInHole(mc.player, BlockType.Resistance)) playerHealth -= holeOffset.get();
            for (ItemStack stack: mc.player.getArmorItems()) {
                if (stack.getItem() instanceof ArmorItem) continue;
                playerHealth -= armorOffset.get();
                if (playerHealth <= minHealth.get()) return;
            }

//            if (fallingCheck.get()) playerHealth -= DamageUtils.getFallDamage();
            if (playerHealth <= minHealth.get()) return;
        }

        findItem(preferCrapples.get() ? Items.GOLDEN_APPLE : Items.ENCHANTED_GOLDEN_APPLE);
        if (!result.found()) {
            if (toHold == Items.GOLDEN_APPLE) findItem(Items.ENCHANTED_GOLDEN_APPLE);
            if (toHold == Items.ENCHANTED_GOLDEN_APPLE) findItem(Items.GOLDEN_APPLE);
        }

        if (result.found() && (!onlyInHoles.get() || CombatUtil.isInHole(mc.player, BlockType.Resistance))) {
            if (mc.options.useKey.isPressed() && rightClickGap.get()) return;
            if ((mainItem instanceof AxeItem || mainItem instanceof SwordItem) && alwaysOnWeapon.get()) return;
        }
    }

    private void findItem(Item item) {
        toHold = item;

        result = InvUtils.find(stack -> stack.getItem() == item, 37, 40);
        if (result.found()) return;
        result = InvUtils.find(stack -> stack.getItem() == item, 9, 35);
        if (result.found() || !useHotbar.get()) return;
        result = InvUtils.find(stack -> stack.getItem() == item, 0, 8);
    }

    private boolean isWearingElytra() {
        return mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
    }

    public enum TotemMode {
        Smart,
        Strict
    }
}
