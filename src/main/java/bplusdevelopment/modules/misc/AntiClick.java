package bplusdevelopment.modules.misc;

import bplusdevelopment.modules.AddModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class AntiClick extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("What blocks to prevent from being clicked on.")
            .filter(this::blockFilter)
            .build());

    public AntiClick() {
        super(AddModule.BANANAMINUS, "anti-click", "Prevents clicking on openable blocks.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.world == null) return;
        if (mc.player != null && mc.player.isSneaking()) return;
        if (!(event.packet instanceof PlayerInteractBlockC2SPacket)) return;

        BlockPos blockPos = ((PlayerInteractBlockC2SPacket) event.packet).getBlockHitResult().getBlockPos();
        if (blocks.get().contains(mc.world.getBlockState(blockPos).getBlock())) event.cancel();
    }

    private boolean blockFilter(Block block) {
        return block instanceof AnvilBlock ||
                block instanceof CraftingTableBlock ||
                block instanceof ChestBlock ||
                block instanceof BarrelBlock ||
                block instanceof EnderChestBlock ||
                block instanceof ShulkerBoxBlock ||
                block instanceof FurnaceBlock ||
                block instanceof LoomBlock ||
                block instanceof CartographyTableBlock ||
                block instanceof GrindstoneBlock ||
                block instanceof StonecutterBlock ||
                block instanceof BlastFurnaceBlock ||
                block instanceof SmokerBlock ||
                block instanceof HopperBlock ||
                block instanceof DispenserBlock ||
                block instanceof LecternBlock ||
                block instanceof BeaconBlock ||
                block instanceof EnchantingTableBlock;
    }
}
