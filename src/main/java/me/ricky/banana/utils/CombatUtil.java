package me.ricky.banana.utils;

import me.ricky.banana.enums.BlockType;
import me.ricky.banana.enums.TrapType;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CombatUtil {
    private static final BlockPos.Mutable testPos = new BlockPos.Mutable();
    private static final List<BlockPos> positions = new ArrayList<>();

    // Anticheat checks

    // isInRange(), canSee() etc. will go here at some point

    // Player checks

    public static boolean isInHole(PlayerEntity player, BlockType blockType) {
        positions.clear();
        positions.addAll(DynamicUtil.feetPos(player));
        positions.addAll(DynamicUtil.underPos(player));

        for (BlockPos pos: positions) if (!blockType.resists(pos)) return false;
        return true;
    }

    public static boolean isTrapped(PlayerEntity player, BlockType blockType, TrapType trapType) {
        positions.clear();

        if (trapType.face()) positions.addAll(DynamicUtil.facePos(player));
        if (trapType.top()) positions.addAll(DynamicUtil.topPos(player));

        for (BlockPos pos: positions) if (!blockType.resists(pos)) return false;
        return true;
    }

    public static boolean isWebbed(PlayerEntity player) {
        Box playerBox = player.getBoundingBox().contract(0.001);

        for (int i = MathHelper.floor(playerBox.minX); i <= MathHelper.floor(playerBox.maxX); i++) {
            for (int j = MathHelper.floor(playerBox.minY); j <= MathHelper.floor(playerBox.maxY); j++) {
                for (int k = MathHelper.floor(playerBox.minZ); k <= MathHelper.floor(playerBox.maxZ); k++) {
                    testPos.set(i, j, k);
                    if (mc.world.getBlockState(testPos).getBlock() == Blocks.COBWEB) return true;
                }
            }
        }

        return false;
    }

    public static boolean isPhased(PlayerEntity player) {
        Box playerBox = player.getBoundingBox();
        double maxY = Math.min(playerBox.maxY, playerBox.minY + 1);
        playerBox = playerBox.withMaxY(maxY).contract(0.001);

        int solids = 0, total = 0;

        for (int i = MathHelper.floor(playerBox.minX); i <= MathHelper.floor(playerBox.maxX); i++) {
            for (int j = MathHelper.floor(playerBox.minY); j <= MathHelper.floor(playerBox.maxY); j++) {
                for (int k = MathHelper.floor(playerBox.minZ); k <= MathHelper.floor(playerBox.maxZ); k++) {
                    testPos.set(i, j, k);
                    total += 1;

                    BlockState state = mc.world.getBlockState(testPos);
                    if (!((AbstractBlockAccessor) state.getBlock()).isCollidable()) continue;
                    if (state.getCollisionShape(mc.world, testPos).isEmpty()) continue;

                    Box stateBox = state.getCollisionShape(mc.world, testPos).getBoundingBox();
                    stateBox = stateBox.offset(testPos.getX(), testPos.getY(), testPos.getZ());

                    if (stateBox.intersects(playerBox)) solids += 1;
                }
            }
        }

        return (double) solids / total >= 0.75;
    }
}
