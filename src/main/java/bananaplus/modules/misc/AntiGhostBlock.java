package bananaplus.modules.misc;

import bananaplus.modules.AddModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AntiGhostBlock extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> onlyBP = sgGeneral.add(new BoolSetting.Builder()
            .name("only-blast-proof")
            .description("Only checks for blast proof blocks to limit spamming packets")
            .defaultValue(true)
            .build());

    private final Setting<Integer> underFeet = sgGeneral.add(new IntSetting.Builder()
            .name("under-feet")
            .description("How many blocks under your feet it should start counting for horizontal")
            .defaultValue(0)
            .sliderRange(-5,5)
            .build());

    private final Setting<Integer> horizontalRange = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-range")
            .defaultValue(4)
            .sliderRange(1,6)
            .build());

    private final Setting<Integer> verticalRange = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-range")
            .defaultValue(4)
            .sliderRange(1, 6)
            .build());

    public final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-toggle")
            .description("Automatically turns off after checking for ghost blocks")
            .defaultValue(true)
            .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Tick delay for checking for ghost blocks.")
            .defaultValue(200)
            .sliderRange(1,2000)
            .build());

    public AntiGhostBlock() {
        super(AddModule.MISC, "anti-ghost-block", "Tries to remove nearby ghost blocks.");
    }

    private static final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private int ticks;

    @Override
    public void onActivate() {
       ticks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player.getAbilities().creativeMode) return;

        if (ticks > 0) ticks--;
        else {
            doCheck();
            ticks = delay.get();
        }

       if (autoToggle.get()) {
           info("Done checking, disabling.");
           toggle();
       }
    }

    private void doCheck() {
        ClientPlayNetworkHandler conn = mc.getNetworkHandler();
        if (conn == null) return;

        BlockPos pos = mc.player.getBlockPos();
        for (int dz = -horizontalRange.get(); dz <= horizontalRange.get(); dz++)
            for (int dx = -horizontalRange.get(); dx <= horizontalRange.get(); dx++)
                for (int dy = -verticalRange.get(); dy <= verticalRange.get(); dy++) {
                    blockPos.set(pos.getX() + dx, (pos.getY() + underFeet.get()) + dy, pos.getZ() + dz);
                    BlockState blockState = mc.world.getBlockState(blockPos);
                    if (!blockState.isAir() && !blockState.isOf(Blocks.BEDROCK)
                            && ((blockState.getBlock().getBlastResistance() >= 600 && onlyBP.get()) || !onlyBP.get())) {
                        if (debug.get()) info(String.valueOf(blockPos));
                        PlayerActionC2SPacket packet = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK
                                ,new BlockPos(pos.getX() + dx, (pos.getY() + underFeet.get()) + dy, pos.getZ() + dz), Direction.UP);

                        conn.sendPacket(packet);
                    }
                }
    }
}
