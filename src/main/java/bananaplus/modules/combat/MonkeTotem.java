package bananaplus.modules.combat;

import bananaplus.BananaPlus;
import bananaplus.utils.BDamageUtils;
import bananaplus.utils.BEntityUtils;
import bananaplus.utils.TimerUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.SlotActionType;

public class MonkeTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCursor = settings.createGroup("Cursor");
    private final SettingGroup sgArmor = settings.createGroup("Armor Modifier");
    private final SettingGroup sgHole = settings.createGroup("In Hole Modifier");

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Determines when to hold a totem, strict will always hold.")
            .defaultValue(Mode.Strict)
            .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> allowHotbarPickup = sgGeneral.add(new BoolSetting.Builder()
            .name("allow-hotbar-pickup")
            .description("Allow totems to be picked up from hotbar.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The milliseconds between slot movements.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0,300)
            .build()
    );

    private final Setting<Double> redHealth = sgGeneral.add(new DoubleSetting.Builder()
            .name("red-health")
            .description("The maximum red health to be considered.")
            .defaultValue(10)
            .range(0,20)
            .sliderMax(20)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Double> yellowHealth = sgGeneral.add(new DoubleSetting.Builder()
            .name("yellow-health")
            .description("The maximum yellow health to be considered.")
            .defaultValue(10)
            .range(0,16)
            .sliderMax(16)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Double> minHealth = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum total health to hold a totem at.")
            .defaultValue(14)
            .range(0,36)
            .sliderMax(36)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Boolean> crystals = sgGeneral.add(new BoolSetting.Builder()
            .name("crystals")
            .description("Will hold a totem when a crystal damage could kill you.")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Double> explosionRadius = sgGeneral.add(new DoubleSetting.Builder()
            .name("explosion-radius")
            .description("Explosion radius of crystals to calculate for that will damage you.")
            .defaultValue(12)
            .sliderRange(1,12)
            .range(1,12)
            .visible(() -> mode.get() == Mode.Smart && crystals.get())
            .build()
    );

    private final Setting<Boolean> swords = sgGeneral.add(new BoolSetting.Builder()
            .name("swords")
            .description("Will hold a totem when a player sword damage could kill you.")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Double> swordsRadius = sgGeneral.add(new DoubleSetting.Builder()
            .name("swords")
            .description("Player radius holding swords to calculate for that will damage you.")
            .defaultValue(5)
            .sliderRange(1, 6.5)
            .range(1, 6.5)
            .visible(() -> mode.get() == Mode.Smart && swords.get())
            .build()
    );

    // Cursor

    private final Setting<Boolean> antiCursorStack = sgCursor.add(new BoolSetting.Builder()
            .name("anti-cursor-stack")
            .description("Puts back items on your cursor back to your inventory after popping.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> antiCursorStackMargin = sgCursor.add(new IntSetting.Builder()
            .name("stack-margin")
            .description("The milliseconds margin after you pop to allow putting an item to your cursor to your inventory.")
            .defaultValue(500)
            .min(0)
            .sliderRange(0,500)
            .visible(antiCursorStack::get)
            .build()
    );

    private final Setting<Boolean> allowHotbarReturn = sgCursor.add(new BoolSetting.Builder()
            .name("allow-hotbar-return")
            .description("Allow items to be moved back to hotbar.")
            .defaultValue(false)
            .visible(antiCursorStack::get)
            .build()
    );

    // Armor

    private final Setting<Double> missingHelmet = sgArmor.add(new DoubleSetting.Builder()
            .name("missing-helmet")
            .description("The minimum total health to increase by if your helmet is missing.")
            .defaultValue(2)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Double> missingChestplate = sgArmor.add(new DoubleSetting.Builder()
            .name("missing-chestplate")
            .description("The minimum total health to increase by if your chestplate is missing.")
            .defaultValue(3)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Double> missingLeggings = sgArmor.add(new DoubleSetting.Builder()
            .name("missing-leggings")
            .description("The minimum total health to increase by if your leggings are missing.")
            .defaultValue(3)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Double> missingBoots = sgArmor.add(new DoubleSetting.Builder()
            .name("missing-boots")
            .description("The minimum total health to increase by if your boots are missing.")
            .defaultValue(2)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    // Hole

    private final Setting<Double> inSingleHole = sgHole.add(new DoubleSetting.Builder()
            .name("in-single-hole")
            .description("The minimum total health to decrease by if your are in a surround.")
            .defaultValue(1)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Double> inSingleBedrock = sgHole.add(new DoubleSetting.Builder()
            .name("in-single-bedrock")
            .description("The minimum total health to decrease by if your are in a bedrock hole.")
            .defaultValue(2)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Double> inDoubleHole = sgHole.add(new DoubleSetting.Builder()
            .name("in-double-hole")
            .description("The minimum total health to decrease by if your are in a double hole.")
            .defaultValue(0.5)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Double> inDoubleBedrock = sgHole.add(new DoubleSetting.Builder()
            .name("in-double-bedrock")
            .description("The minimum total health to decrease by if your are in a double bedrock hole.")
            .defaultValue(1)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    public MonkeTotem() {
        super(BananaPlus.COMBAT, "monke-totem", "Automatically puts a totem in your offhand.");
    }

    private final TimerUtils offhandTimer = new TimerUtils();
    private final TimerUtils poppedTimer = new TimerUtils();
    private boolean locked;
    private int totems;

    private float helmetModifier;
    private float chestplateModifier;
    private float leggingsModifier;
    private float bootsModifier;


    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        FindItemResult result = InvUtils.find(Items.TOTEM_OF_UNDYING);
        totems = result.count();

        // You can't swap items if you have the screen open, it will crash
        if (mc.currentScreen instanceof GenericContainerScreen) return;

        // Check if your health is low or mode is on strict
        boolean low = Math.min(mc.player.getHealth(), redHealth.get()) + Math.min(mc.player.getAbsorptionAmount(), yellowHealth.get()) - BDamageUtils.possibleHealthReductions(crystals.get(), explosionRadius.get().floatValue(), swords.get(), swordsRadius.get().floatValue()) <= minHealth.get() + armorModifier() - holeModifier();
        locked = (mode.get() == Mode.Strict || (mode.get() == Mode.Smart && low) && totems > 0);

        // We find a totem from the inventory not only if our health is low, so we know exactly where to pick up another totem immediately
        int totemSlot = -1;
        for (int i = allowHotbarPickup.get() ? 0 : 9; i <= 35; i++) {
            if (mc.player.getInventory().getStack(i).getItem().equals(Items.TOTEM_OF_UNDYING)) {
                totemSlot = i;
                break;
                // Breaking the process since it doesn't need to look anymore
            }
        }

        // Being the process if above condition is true, delay is met, and offhand is not a totem
        if (locked && offhandTimer.passedMillis(delay.get()) && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            // Move the totem to offhand
            if (totemSlot != -1) {
                InvUtils.move().from(totemSlot).to(45);
                offhandTimer.reset();
            }
        }

        if (antiCursorStack.get()) {
            // We need to find an empty slot in our inventory first otherwise we will just pick up another item, also finding this outside of the condition
            int returnSlot = -1;
            for (int i = allowHotbarReturn.get() ? 0 : 9; i <= 35; i++) {
                if (mc.player.getInventory().getStack(i).isEmpty()) {
                    returnSlot = i;
                    break;
                    // Breaking the process once again here
                }
            }

            // Now we need to put an item back to our inventory if there was an item on our cursor slot so when we chain pop we don't totem fail
            // Because meteor's inv utils of moving the item will check first whether the cursor slot is empty or not and return
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty() && !poppedTimer.passedMillis(antiCursorStackMargin.get())) {

                // Putting the original item back to our inventory
                if (returnSlot != -1)
                    mc.interactionManager.clickSlot(0, returnSlot, 0, SlotActionType.PICKUP, mc.player);
            }
        }

        // Debug

        // As stated in the entity utils, u have to check for bedrock first if the condition is non mutually exclusive
        // Also you have to check for single holes first before doing the doubles

        if (debug.get() && mode.get() == Mode.Smart) {
            if (inSingleBedrock.get() > 0 && BEntityUtils.isSurrounded(mc.player, BEntityUtils.BlastResistantType.Unbreakable)) info("Is in single bedrock");
            else if (inSingleHole.get() > 0 && BEntityUtils.isSurrounded(mc.player, BEntityUtils.BlastResistantType.Any)) info("In in single hole");
            else if (inDoubleBedrock.get() > 0 && BEntityUtils.isInHole(mc.player, true, BEntityUtils.BlastResistantType.Unbreakable)) info("Is in double bedrock");
            else if (inDoubleHole.get() > 0 && BEntityUtils.isInHole(mc.player, true, BEntityUtils.BlastResistantType.Any)) info("Is in double hole");

            warning("True min health = " + minHealth.get() + " + " + armorModifier() + " - " + holeModifier() + " = " + (minHealth.get() + armorModifier() - holeModifier()));
        }
    }

    private float armorModifier() {
        // Checking if it should decrease min health in smart mode because a piece of armor is missing
        Item helmet = mc.player.getInventory().getArmorStack(3).getItem();
        Item chestplate = mc.player.getInventory().getArmorStack(2).getItem();
        Item leggings = mc.player.getInventory().getArmorStack(1).getItem();
        Item boots = mc.player.getInventory().getArmorStack(0).getItem();

        // Changed it to like this instead of using && to save fps
        if (missingHelmet.get() > 0) {
             if (BEntityUtils.isHelmet(helmet)) helmetModifier = 0;
             else {
                 helmetModifier = missingHelmet.get().floatValue();
                 if (debug.get() && mode.get() == Mode.Smart) info("Helmet missing");
             }
        }

        if (missingChestplate.get() > 0) {
            if (BEntityUtils.isChestplate(chestplate)) chestplateModifier = 0;
            else {
                chestplateModifier = missingHelmet.get().floatValue();
                if (debug.get() && mode.get() == Mode.Smart) info("Chestplate missing");
            }
        }

        if (missingLeggings.get().floatValue() > 0) {
            if (BEntityUtils.isLeggings(leggings)) leggingsModifier = 0;
            else {
                leggingsModifier = missingLeggings.get().floatValue();
                if (debug.get() && mode.get() == Mode.Smart) info("Leggings missing");
            }
        }

        if (missingBoots.get() > 0) {
            if (BEntityUtils.isBoots(boots)) bootsModifier = 0;
            else {
                bootsModifier = missingBoots.get().floatValue();
                if (debug.get() && mode.get() == Mode.Smart) info("Boots missing");
            }
        }

        return helmetModifier + chestplateModifier + leggingsModifier + bootsModifier;
    }

    private float holeModifier() {
        // Todo : implement the ping sync util from serverutils

        if (inSingleBedrock.get() > 0 && BEntityUtils.isSurrounded(mc.player, BEntityUtils.BlastResistantType.Unbreakable)) return inSingleBedrock.get().floatValue();
        else if (inSingleHole.get() > 0 && BEntityUtils.isSurrounded(mc.player, BEntityUtils.BlastResistantType.Any)) return inSingleHole.get().floatValue();
        else if (inDoubleBedrock.get() > 0 && BEntityUtils.isInHole(mc.player, true, BEntityUtils.BlastResistantType.Unbreakable)) return inDoubleBedrock.get().floatValue();
        else if (inDoubleHole.get() > 0 && BEntityUtils.isInHole(mc.player, true, BEntityUtils.BlastResistantType.Any)) return inDoubleHole.get().floatValue();

        return 0;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);
        if (entity == null || !(entity.equals(mc.player))) return;

        poppedTimer.reset();
    }

    public boolean isLocked() {
        return isActive() && locked;
    }

    @Override
    public String getInfoString() {
        return String.valueOf(totems);
    }

    public enum Mode {
        Smart,
        Strict
    }
}