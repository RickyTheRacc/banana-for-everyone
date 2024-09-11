package me.ricky.bananaplus.utils;

import me.ricky.bananaplus.enums.BlockType;
import me.ricky.bananaplus.systems.BananaUtils;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class CombatUtil extends BananaUtils {
    private static final BlockPos.Mutable testPos = new BlockPos.Mutable();

    public static boolean isSurrounded(PlayerEntity player, BlockType type) {
        List<BlockPos> posList = new ArrayList<>();
        posList.addAll(DynamicUtil.feetPos(player));
        posList.addAll(DynamicUtil.underPos(player));

        return posList.stream().allMatch(type::resists);
    }

    public static boolean isTopTrapped(PlayerEntity player, BlockType type) {
        for (BlockPos pos: DynamicUtil.topPos(player)) {
            if (!type.resists(pos)) return false;
        }

        return true;
    }

    public static boolean isFaceTrapped(PlayerEntity player, BlockType type) {
        for (BlockPos pos: DynamicUtil.facePos(player)) {
            if (!type.resists(pos)) return false;
        }

        return true;
    }

    public static boolean isFullTrapped(PlayerEntity player, BlockType type ) {
        List<BlockPos> posList = new ArrayList<>();
        posList.addAll(DynamicUtil.topPos(player));
        posList.addAll(DynamicUtil.facePos(player));

        for (BlockPos pos: posList) {
            if (!type.resists(pos)) return false;
        }

        return true;
    }

    public static boolean isPhased(PlayerEntity player) {
        Box box = box(player).contract(0.001);

        for (double[] point : DynamicUtil.fourWay(box)) {
            testPos.set(point[0], box.minY + 0.4, point[1]);

            BlockState state = mc.world.getBlockState(testPos);
            if (!((AbstractBlockAccessor) state.getBlock()).isCollidable()) continue;
            if (state.getCollisionShape(mc.world, testPos).isEmpty()) continue;

            Box stateBox = state.getCollisionShape(mc.world, testPos).getBoundingBox();
            stateBox = stateBox.offset(testPos.getX(), testPos.getY(), testPos.getZ());

            if (stateBox.intersects(box)) return true;
        }

        return false;
    }

    public static boolean isWebbed(PlayerEntity player) {
        Box playerBox = box(player).contract(0.001);

        for (int i = (int) Math.floor(playerBox.minX); i <= Math.floor(playerBox.maxX); i++) {
            for (int j = (int) Math.floor(playerBox.minY); j <= Math.floor(playerBox.maxY); j++) {
                for (int k = (int) Math.floor(playerBox.minZ); k <= Math.floor(playerBox.maxZ); k++) {
                    testPos.set(i, j, k);
                    if (mc.world.getBlockState(testPos).isOf(Blocks.COBWEB)) return true;
                }
            }
        }

        return false;
    }
}
