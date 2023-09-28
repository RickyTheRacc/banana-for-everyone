package bananaplus.fixedutils;

import bananaplus.enums.BlockType;
import bananaplus.enums.TrapType;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;
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

    public static Pair<Boolean, Boolean> isPhased(PlayerEntity player) {
        Box playerBox = player.getBoundingBox().contract(0.001);
        playerBox = playerBox.withMaxY(Math.min(playerBox.maxY, playerBox.minY + 1));

        int minX = MathHelper.floor(playerBox.minX);
        int minY = MathHelper.floor(playerBox.minY);
        int minZ = MathHelper.floor(playerBox.minZ);
        int maxX = MathHelper.floor(playerBox.maxX);
        int maxY = MathHelper.floor(playerBox.maxY);
        int maxZ = MathHelper.floor(playerBox.maxZ);

        int solids = 0, total = 0;
        boolean isWebbed = false;

        for (int i = minX; i <= maxX; i++) {
            for (int j = minY; j <= maxY; j++) {
                for (int k = minZ; k <= maxZ; k++) {
                    testPos.set(i, j, k);
                    total++;

                    BlockState state = mc.world.getBlockState(testPos);
                    if (state.getBlock() == Blocks.COBWEB) {
                        isWebbed = true;
                        solids++;
                    }

                    if (!((AbstractBlockAccessor) state.getBlock()).isCollidable()) continue;

                    Box stateBox = state.getOutlineShape(mc.world, testPos).getBoundingBox();
                    stateBox = stateBox.offset(testPos.getX(), testPos.getY(), testPos.getZ());

                    if (stateBox.intersects(playerBox)) solids++;
                }
            }
        }

        boolean isPhased = (double) solids / total >= 0.75;
        return new Pair<>(isPhased, isWebbed);
    }
}
