package bananaplus.modules.combat;

import bananaplus.BananaPlus;
import bananaplus.utils.BPlusEntityUtils;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;

public class QuiverRewrite extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayer = settings.createGroup("Player");


    // General Settings
    public final Setting<Keybind> strengthBind = sgGeneral.add(new KeybindSetting.Builder()
            .name("strength")
            .description("What key to trigger using a strength arrow.")
            .defaultValue(Keybind.none())
            .build()
    );

    public final Setting<Keybind> speedBind = sgGeneral.add(new KeybindSetting.Builder()
            .name("speed")
            .description("What key to trigger using a speed arrow.")
            .defaultValue(Keybind.none())
            .build()
    );

    private final Setting<Integer> effectDelay = sgGeneral.add(new IntSetting.Builder()
            .name("use-delay")
            .description("How long to wait between shooting arrows.")
            .defaultValue(2)
            .range(0,10)
            .sliderRange(0,10)
            .build()
    );

    private final Setting<Boolean> autoSwap = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-swap")
            .description("Will move a bow from your inventory to and from your hotbar as required.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> bowSlot = sgGeneral.add(new IntSetting.Builder()
            .name("bow-slot")
            .description("What slot to move a bow to if no bow is found.")
            .defaultValue(9)
            .range(1,9)
            .sliderRange(1,9)
            .visible(autoSwap::get)
            .build()
    );

    private final Setting<Boolean> tpsSync = sgGeneral.add(new BoolSetting.Builder()
            .name("TPS-sync")
            .description("Sync the module's delays with the server's TPS.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Send messages in the chat when quivering.")
            .defaultValue(true)
            .build()
    );


    // Player Settings
    private final Setting<Boolean> onlyInHole = sgPlayer.add(new BoolSetting.Builder()
            .name("only-in-holes")
            .description("Stops you from quivering when not in a hole.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgPlayer.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Stops you from quivering when not on the ground.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> center = sgPlayer.add(new BoolSetting.Builder()
            .name("center")
            .description("Center the player before shooting an arrow.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> crouch = sgPlayer.add(new BoolSetting.Builder()
            .name("crouch")
            .description("Crouch while shooting the arrow to reduce charge time.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> minHealth = sgPlayer.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("How much health you must have to throw XP.")
            .defaultValue(10)
            .range(0,36)
            .sliderRange(0,36)
            .build()
    );


    public QuiverRewrite() {
        super(BananaPlus.COMBAT, "quiver-plus", "Shoot yourself with effect arrows.");
    }

    private double delay;
    private double cooldown;
    private int prevBowSlot;


    @EventHandler
    public void onKeyPressed(KeyEvent event) {
        if ((strengthBind.get().isPressed() || speedBind.get().isPressed()) && mc.currentScreen == null) {
            if (!checkHead()) {
                if (chatInfo.get()) error("Not enough space to quiver.");
                return;
            }

            if (onlyOnGround.get() && !mc.player.isOnGround()) {
                if (chatInfo.get()) error("You aren't on the ground.");
                return;
            }

            if (!BPlusEntityUtils.isInHole(mc.player, true, BPlusEntityUtils.BlastResistantType.Any) && onlyInHole.get()) {
                if (chatInfo.get()) error("You aren't in a hole.");
                return;
            }

            if (mc.player.getHealth() < minHealth.get()) {
                if (chatInfo.get()) error("Your health is too low.");
                return;
            }

            FindItemResult bow = InvUtils.find(Items.BOW);
            if (!bow.isHotbar() && !autoSwap.get() || !bow.found()) {
                if (chatInfo.get()) error("Couldn't find a bow.");
                return;
            }


            // Bow Prep
            if (center.get()) PlayerUtils.centerPlayer();

            if (!bow.isHotbar()) {
                prevBowSlot = bow.slot();
                InvUtils.move().from(bow.slot()).to((bowSlot.get() - 1));
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (cooldown <= 0) {
            cooldown = 0;
            return;
        } else cooldown--;



    }


    private boolean checkHead() {
        BlockState pos1 = mc.world.getBlockState(mc.player.getBlockPos().add(0, 2, 0));
        BlockState pos2 = mc.world.getBlockState(mc.player.getBlockPos().add(0, 3, 0));

        boolean air1 = !((AbstractBlockAccessor)pos1.getBlock()).isCollidable();
        boolean air2 = !((AbstractBlockAccessor)pos2.getBlock()).isCollidable();

        return (air1 & air2);
    }
}
