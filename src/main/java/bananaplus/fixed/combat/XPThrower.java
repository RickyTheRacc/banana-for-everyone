package bananaplus.fixed.combat;

import bananaplus.BananaPlus;
import bananaplus.enums.SwitchMode;
import bananaplus.utils.BEntityUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class XPThrower extends Module {
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

    private final Setting<Boolean> noGapSwitch = sgSafety.add(new BoolSetting.Builder()
            .name("no-gap-switch")
            .description("Whether to switch to XP if you're holding a gap.")
            .defaultValue(true)
            .visible(() -> switchMode.get() == SwitchMode.Normal)
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

    private final Setting<Boolean> dynamic = sgSafety.add(new BoolSetting.Builder()
            .name("dynamic")
            .description("Allow holes of other sizes than 1x1.")
            .defaultValue(true)
            .visible(onlyInHole::get)
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

    public XPThrower() {
        super(BananaPlus.FIXED, "XP-thrower", "Throw XP bottles to repair your armor and tools.");
    }

    private double delay = 0;

    @Override
    public void onActivate() {
        delay = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        delay -= (TickRate.INSTANCE.getTickRate() / 20.0);
        if (delay > 0 || shouldWait()) return;
        delay = throwDelay.get();

        for (int i = 0; i < 4; i++) {
            if (isRepaired(mc.player.getInventory().getArmorStack(i))) continue;
            FindItemResult xpBottles = InvUtils.find(Items.EXPERIENCE_BOTTLE);
            if (!xpBottles.found() || !xpBottles.isHotbar() && switchMode.get() != SwitchMode.Inventory) return;

            if (xpBottles.isOffhand() || xpBottles.isMainHand())  {
                mc.interactionManager.interactItem(mc.player, xpBottles.getHand());
                break;
            }

            if (noGapSwitch.get() && isHoldingGapples()) return;
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

            break;
        }
    }

    private boolean isHoldingGapples() {
        if (mc.player.getMainHandStack().getItem() == Items.GOLDEN_APPLE) return true;
        return mc.player.getMainHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }

    private boolean shouldWait() {
        if (!throwBind.get().isPressed() || mc.currentScreen != null) return true;
        if (PlayerUtils.getTotalHealth() <= minHealth.get()) return true;
        if (usePause.get() && mc.player.isUsingItem()) return true;
        if (minePause.get() && mc.interactionManager.isBreakingBlock()) return true;

        if (onlyOnGround.get() && !mc.player.isOnGround()) return true;
        return (onlyInHole.get() && BEntityUtils.isInHole(mc.player, dynamic.get(), BEntityUtils.BlastResistantType.Any));
    }

    private boolean isRepaired(ItemStack stack) {
        if (stack.getMaxDamage() <= 0 || EnchantmentHelper.getLevel(Enchantments.MENDING, stack) == 0) return true;
        return ((double) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()) * 100 > threshold.get();
    }
}
