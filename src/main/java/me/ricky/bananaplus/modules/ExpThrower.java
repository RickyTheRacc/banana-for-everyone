package me.ricky.bananaplus.modules;

import me.ricky.bananaplus.BananaPlus;
import me.ricky.bananaplus.enums.BlockType;
import me.ricky.bananaplus.enums.SwitchMode;
import me.ricky.bananaplus.utils.CombatUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

@SuppressWarnings("ConstantConditions")
public class ExpThrower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgThrowing = settings.createGroup("Throwing");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    // General

    public final Setting<Keybind> throwBind = sgGeneral.add(new KeybindSetting.Builder()
        .name("keybind")
        .description("The keybind to throw XP.")
        .defaultValue(Keybind.none())
        .build()
    );

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
        .name("threshold")
        .description("The maximum durability to repair items to.")
        .defaultValue(80)
        .range(1,99)
        .sliderRange(1,99)
        .build()
    );

    // Throwing

    private final Setting<Integer> throwDelay = sgThrowing.add(new IntSetting.Builder()
        .name("throw-delay")
        .description("How fast to throw XP.")
        .defaultValue(1)
        .range(0,20)
        .sliderRange(0,20)
        .build()
    );

    private final Setting<SwitchMode> switchMode = sgThrowing.add(new EnumSetting.Builder<SwitchMode>()
        .name("switch-mode")
        .description("How to switch to the XP.")
        .defaultValue(SwitchMode.Silent)
        .build()
    );

    private final Setting<Boolean> noGapSwitch = sgThrowing.add(new BoolSetting.Builder()
        .name("no-gap-switch")
        .description("Whether to switch to XP if you're holding a gap.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> lookDown = sgThrowing.add(new BoolSetting.Builder()
        .name("look-down")
        .description("Forces you to rotate downwards when throwing XP.")
        .defaultValue(true)
        .build()
    );

    // Safety

    private final Setting<Boolean> onlyOnGround = sgSafety.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only activate when you are on the ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyInHole = sgSafety.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Only activate when you are in a hole.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> usePause = sgSafety.add(new BoolSetting.Builder()
        .name("pause-on-use")
        .description("Whether to pause while using an item.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> minePause = sgSafety.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Whether to pause while eating.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> minHealth = sgSafety.add(new DoubleSetting.Builder()
        .name("min-health")
        .description("How much health you must have to throw XP.")
        .defaultValue(10)
        .range(1,36)
        .sliderRange(1,36)
        .build()
    );

    private double delay = 0.0;

    public ExpThrower() {
        super(BananaPlus.CATEGORY, "b+-exp-thrower", "Throw EXP bottles to repair your armor and tools.");
    }

    @Override
    public void onActivate() {
        delay = 0.0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        delay -= TickRate.INSTANCE.getTickRate() / 20.0;
        if (delay > 0 || isRepaired() || shouldWait()) return;

        FindItemResult xpBottles = InvUtils.find(Items.EXPERIENCE_BOTTLE);
        if (xpBottles.isOffhand() || xpBottles.isMainHand())  {
            mc.interactionManager.interactItem(mc.player, xpBottles.getHand());
            return;
        }

        if (!xpBottles.found() || !xpBottles.isHotbar() && switchMode.get() != SwitchMode.Inventory) return;
        int selectedSlot = mc.player.getInventory().selectedSlot;

        switch (switchMode.get()) {
            case Normal, Silent -> InvUtils.swap(xpBottles.slot(), switchMode.get() == SwitchMode.Silent);
            case Inventory -> InvUtils.quickSwap().fromId(selectedSlot).to(xpBottles.slot());
        }

        if (lookDown.get()) Rotations.rotate(mc.player.getYaw(), 90);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        delay = throwDelay.get();

        switch (switchMode.get()) {
            case Silent -> InvUtils.swapBack();
            case Inventory -> InvUtils.quickSwap().fromId(selectedSlot).to(xpBottles.slot());
        }
    }

    private boolean isHoldingGapples() {
        if (mc.player.getMainHandStack().getItem() == Items.GOLDEN_APPLE) return true;
        return mc.player.getMainHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }

    private boolean shouldWait() {
        if (!throwBind.get().isPressed() || mc.currentScreen != null) return true;
        if (PlayerUtils.getTotalHealth() <= minHealth.get()) return true;

        if (noGapSwitch.get() && isHoldingGapples()) return true;
        if (usePause.get() && mc.player.isUsingItem()) return true;
        if (minePause.get() && mc.interactionManager.isBreakingBlock()) return true;

        if (onlyOnGround.get() && !mc.player.isOnGround()) return true;
        return onlyInHole.get() && !CombatUtil.isSurrounded(mc.player, BlockType.Resistance);
    }

    private boolean isRepaired() {
        for (int i = 0; i < 4; i++) {
            ItemStack stack = mc.player.getInventory().getArmorStack(i);

            if (stack.getMaxDamage() <= 0 || !Utils.hasEnchantment(stack, Enchantments.MENDING)) continue;
            if (((double) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()) * 100 <= threshold.get()) return false;
        }

        return true;
    }
}