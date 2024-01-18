package me.ricky.banana.utils;

import me.ricky.banana.enums.BlockType;
import me.ricky.banana.systems.BananaUtils;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public class CombatUtils extends BananaUtils {
    private static final BlockPos.Mutable testPos = new BlockPos.Mutable();

    public boolean isSurrounded(PlayerEntity player, BlockType type) {
        List<BlockPos> posList = new ArrayList<>();
        posList.addAll(DynamicUtils.feetPos(player));
        posList.addAll(DynamicUtils.underPos(player));

        return posList.stream().allMatch(type::resists);
    }

    public boolean isTopTrapped(PlayerEntity player, BlockType type) {
        for (BlockPos pos: DynamicUtils.topPos(player)) {
            if (!type.resists(pos)) return false;
        }

        return true;
    }

    public boolean isFaceTrapped(PlayerEntity player, BlockType type) {
        for (BlockPos pos: DynamicUtils.facePos(player)) {
            if (!type.resists(pos)) return false;
        }

        return true;
    }

    public boolean isFullTrapped(PlayerEntity player, BlockType type ) {
        List<BlockPos> posList = new ArrayList<>();
        posList.addAll(DynamicUtils.topPos(player));
        posList.addAll(DynamicUtils.facePos(player));

        for (BlockPos pos: posList) {
            if (!type.resists(pos)) return false;
        }

        return true;
    }

    public static boolean isBurrowed(PlayerEntity player) {
        Box box = box(player).contract(0.001);

        for (double[] point : DynamicUtils.fourWay(box)) {
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
