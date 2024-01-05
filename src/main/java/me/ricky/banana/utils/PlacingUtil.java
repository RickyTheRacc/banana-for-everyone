package me.ricky.banana.utils;

import me.ricky.banana.enums.SwingMode;
import me.ricky.banana.enums.SwitchMode;
import me.ricky.banana.system.BananaConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlacingUtil {
    private static final HashMap<BlockPos, Long> placedBlocks = new HashMap<>();
    private static final Vec3d vec = new Vec3d(0, 0, 0);
    private static final BlockPos.Mutable pos = new BlockPos.Mutable();

    @PostInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(PlacingUtil.class);
    }

    public static boolean tryPlace(BlockPos pos, FindItemResult result, SwingMode swingMode) {
        if (!shouldPlace(pos)) return false;
        if (!result.found()) return false;

        Hand hand = result.slot() == 40 ? Hand.OFF_HAND : Hand.MAIN_HAND;
        SwitchMode switchMode = BananaConfig.get().switchMode.get();
        if (hand == Hand.MAIN_HAND && (result.slot() < 0 || result.slot() > 8) && switchMode.onlyHotbar()) return false;

        BlockHitResult placeResult = placeResult(pos);
        if (placeResult == null) return false;

        placedBlocks.put(placeResult.getBlockPos(), System.currentTimeMillis());
        placeBlock(result, placeResult, hand, switchMode, swingMode);
        return true;
    }

    public static void forcePlace(BlockPos pos, FindItemResult result, SwingMode swingMode) {
        Hand hand = result.slot() == 40 ? Hand.OFF_HAND : Hand.MAIN_HAND;
        SwitchMode switchMode = BananaConfig.get().switchMode.get();
        if (hand == Hand.MAIN_HAND && (result.slot() < 0 || result.slot() > 8) && switchMode.onlyHotbar()) return;

        BlockHitResult placeResult = placeResult(pos);
        if (placeResult != null) placeBlock(result, placeResult, hand, switchMode, swingMode);
    }

    private static void placeBlock(FindItemResult itemResult, BlockHitResult hitResult, Hand hand, SwitchMode switchMode, SwingMode swingMode) {
        if (BananaConfig.get().blockRotate.get()) Rotations.rotate(Rotations.getYaw(hitResult.getPos()), Rotations.getPitch(hitResult.getPos()), 9999);

        BlockState state = mc.world.getBlockState(hitResult.getBlockPos());
        boolean sneak = !mc.player.isSneaking() && state.onUse(mc.world, mc.player, hand, hitResult) != ActionResult.PASS;
        boolean selected = hand == Hand.OFF_HAND || mc.player.getInventory().selectedSlot == itemResult.slot();

        if (sneak) mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        if (!selected) {
            switch (switchMode) {
                case Normal, Silent -> InvUtils.swap(itemResult.slot(), switchMode == SwitchMode.Silent);
                case Inventory -> InvUtils.quickSwap().fromId(mc.player.getInventory().selectedSlot).to(itemResult.slot());
            }
        }

        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
        swingMode.swing(hand);

        if (sneak) mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        if (hand == Hand.MAIN_HAND && !selected) {
            switch (switchMode) {
                case Silent -> InvUtils.swapBack();
                case Inventory -> InvUtils.quickSwap().fromId(mc.player.getInventory().selectedSlot).to(itemResult.slot());
            }
        }
    }

    public static BlockHitResult placeResult(BlockPos pos) {
        BlockPos placePos = null;
        boolean airPlace = BananaConfig.get().airPlace.get();

        Direction side = bestDirection(pos, airPlace);
        if (side != null) placePos = pos.offset(side.getOpposite());

        if (side == null || placePos == null) {
            if (!airPlace) return null;

            placePos = pos;
            side = (mc.player.getY() > pos.getY()) ? Direction.UP : Direction.DOWN;
        }

        int x = placePos.getX(), y = placePos.getY(), z = placePos.getZ();

        ((IVec3d) vec).set(
            MathHelper.clamp(mc.player.getEyePos().x, x, x + 1),
            MathHelper.clamp(mc.player.getEyePos().x, y, y + 1),
            MathHelper.clamp(mc.player.getEyePos().x, z, z + 1)
        );

        return new BlockHitResult(vec, side, placePos, false);
    }

    public static Direction bestDirection(BlockPos blockPos, boolean airplace) {
        Direction bestDirection = null;
        double bestDistance = 100;

        for (Direction direction : Direction.values()) {
            pos.set(blockPos.offset(direction));

            BlockState state = mc.world.getBlockState(pos);
            if (state.isReplaceable() || !state.getFluidState().isEmpty()) continue;
            if (state.isAir() && !placedBlocks.containsKey(pos) && !airplace) continue;

            ((IVec3d) vec).set(
                (double) pos.getX() + 0.5 + (direction.getOffsetX() * 0.5),
                (double) pos.getY() + 0.5 + (direction.getOffsetY() * 0.5),
                (double) pos.getZ() + 0.5 + (direction.getOffsetZ() * 0.5)
            );

            double distance = mc.player.getEyePos().squaredDistanceTo(vec);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestDirection = direction.getOpposite();
            }
        }

        return bestDirection;
    }

    public static boolean shouldPlace(BlockPos blockPos) {
        if (placedBlocks.containsKey(blockPos)) return false;
        if (!mc.world.getWorldBorder().contains(blockPos)) return false;
        if (mc.world.isOutOfHeightLimit(blockPos)) return false;
        return mc.world.getBlockState(blockPos).isReplaceable();
    }

    @EventHandler
    private static void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof BlockUpdateS2CPacket packet) {
            if (!placedBlocks.containsKey(packet.getPos())) return;
            if (packet.getState().isAir()) return;

            placedBlocks.remove(packet.getPos());
            pos.set(packet.getPos());
            BlockSoundGroup group = packet.getState().getBlock().getSoundGroup(packet.getState());

            RenderSystem.recordRenderCall(() -> mc.world.playSound(
                pos.getX(), pos.getY(), pos.getZ(),
                group.getPlaceSound(), SoundCategory.BLOCKS,
                group.getVolume(), group.getPitch(), true
            ));
        }
    }

    @EventHandler
    private static void onPreTick(TickEvent.Pre event) {
        double latency = Math.max(PlayerUtils.getPing(), 100);

        for (Map.Entry<BlockPos, Long> entry : placedBlocks.entrySet()) {
            if ((double) (System.currentTimeMillis() - entry.getValue()) < latency) continue;
            placedBlocks.remove(entry.getKey());
        }
    }
}
