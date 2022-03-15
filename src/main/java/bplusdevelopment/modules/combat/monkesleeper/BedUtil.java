package bplusdevelopment.modules.combat.monkesleeper;

import bplusdevelopment.utils.BPlusPlayerUtils;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BedItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BedUtil {
    static MonkeSleeper MSleeper = Modules.get().get(MonkeSleeper.class);

    public static void breakBed(BlockPos pos) {
        //if (!(mc.world.getBlockState(pos).getBlock() instanceof BedBlock)) return;

        if (mc.player.isSneaking()) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));

        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false)));
        //mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getEyePos(), Direction.UP, pos, false));

        MSleeper.breakTimer = MSleeper.breakDelay.get();
    }


    // Inventory

    public static void doMove() {
        FindItemResult hotbarBed = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if (!hotbarBed.found()) {
            if (bed.found()) InvUtils.move().from(bed.slot()).toHotbar(MSleeper.autoMoveSlot.get() - 1);
        }
    }

    // Damage Ignores
    public static boolean targetJustPopped() {
        if (MSleeper.targetPopInvincibility.get()) {
            return !MSleeper.targetPoppedTimer.passedMillis(MSleeper.targetPopInvincibilityTime.get());
        }

        return false;
    }

    public static boolean shouldIgnoreSelfPlaceDamage() {
        return (MSleeper.PDamageIgnore.get() == MonkeSleeper.DamageIgnore.Always
                || (MSleeper.selfPopInvincibility.get() && MSleeper.selfPopIgnore.get() != MonkeSleeper.SelfPopIgnore.Break && !MSleeper.selfPoppedTimer.passedMillis(MSleeper.selfPopInvincibilityTime.get())));
    }

    public static boolean shouldIgnoreSelfBreakDamage() {
        return (MSleeper.BDamageIgnore.get() == MonkeSleeper.DamageIgnore.Always
                || (MSleeper.selfPopInvincibility.get() && MSleeper.selfPopIgnore.get() != MonkeSleeper.SelfPopIgnore.Place && !MSleeper.selfPoppedTimer.passedMillis(MSleeper.selfPopInvincibilityTime.get())));
    }

    /*
    public static List<BlockPos> getOffsetPositions(BlockPos blockPos) {
        List<BlockPos> positions = new ArrayList<>();
        for (CardinalDirection direction : CardinalDirection.values()) {
            BlockPos pos = blockPos.offset(direction.toDirection());
            if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                // Check if it can be placed
                if (EntityUtils.intersectsWithEntity(new Box(pos), entity -> !entity.isSpectator() && entity.getType() != EntityType.ITEM)) continue;{
                    positions.add(pos);
                }
            }
        }
        return positions;
    }
     */



    // Incase meteor intersection method isn't fixed
    private static final Box box = new Box(0, 0, 0, 0, 0, 0);

    public static List<BlockPos> getOffsetPositions(BlockPos blockPos) {
        List<BlockPos> positions = new ArrayList<>();
        for (CardinalDirection direction : CardinalDirection.values()) {

            BlockPos pos = blockPos.offset(direction.toDirection());
            Vec3d centeredPos = Vec3d.ofCenter(pos);

            if (mc.player.getEyePos().distanceTo(centeredPos) <= MSleeper.placeRange.get()) {
                if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                    // Check if it can be placed
                    int x = pos.getX();
                    int y = pos.getY();
                    int z = pos.getZ();
                    ((IBox) box).set(x, y, z, x + 1, y + 0.5625, z + 1);

                    if (EntityUtils.intersectsWithEntity(new Box(pos), entity -> !entity.isSpectator() && entity.collides())) continue;

                    positions.add(pos);
                }
            }
        }

        return positions;
    }

    public static BlockPos getClosestOffset(BlockPos blockPos) {
        List<BlockPos> posList = getOffsetPositions(blockPos);
        posList.sort(Comparator.comparingDouble(BPlusPlayerUtils::distanceFromEye));
        return posList.isEmpty() ? null : posList.get(0);
    }

    public static CardinalDirection offsetDirection(BlockPos placePos, BlockPos directedPos) {
        for (CardinalDirection direction : CardinalDirection.values()) {
            if (placePos.offset(direction.toDirection()).equals(directedPos)) return direction;
        }

        return null;
    }

    public static double getDirectedYaw(BlockPos placePos, BlockPos directedPos) {
        switch (offsetDirection(placePos, directedPos)) {
            case North -> {
                return 180;
            }
            case East -> {
                return -90;
            }
            case South -> {
                return 0;
            }
            case West -> {
                return 90;
            }
        }

        return 0;
    }
/*
    south = -45.000 <= x < 45.000
    west = 45.000 <= x < 135.000
    north = -225.000 <= x < -135.000
    east = -135.000 <= x < -45.000
 */

}