package me.ricky.banana.utils;

import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dir;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlockUtil {
    private static final Box testBox = new Box(BlockPos.ORIGIN);
    private static final BlockPos.Mutable testPos = new BlockPos.Mutable();
    private static final Vec3d testVec = new Vec3d(0, 0, 0);

    public static Vec3d closestPoint(Box box) {
        ((IVec3d) testVec).set(
            MathHelper.clamp(mc.player.getX(), box.minX, box.maxX),
            MathHelper.clamp(mc.player.getY(), box.minY, box.maxY),
            MathHelper.clamp(mc.player.getY(), box.minZ, box.maxZ)
        );

        return testVec;
    }

    public static boolean combatFilter(Block block) {
        return block == Blocks.OBSIDIAN
            || block == Blocks.CRYING_OBSIDIAN
            || block instanceof AnvilBlock
            || block == Blocks.NETHERITE_BLOCK
            || block == Blocks.ENDER_CHEST
            || block == Blocks.RESPAWN_ANCHOR
            || block == Blocks.ANCIENT_DEBRIS
            || block == Blocks.ENCHANTING_TABLE;
    }

    public static double getBreakDelta(int slot, BlockState state) {
        if (slot == -1) return 0.0f;
        if (PlayerUtils.getGameMode() == GameMode.CREATIVE) return 1.0f;
        else return BlockUtils.getBreakDelta(slot, state);
    }

    public static HashMap<BlockPos, Integer> excludeSides(List<BlockPos> posList) {
        HashMap<BlockPos, Integer> poses = new HashMap<>();

        for (BlockPos pos: posList) {
            int excludeDir = 0;

            for(Direction direction: Direction.values()) {
                if (!posList.contains(pos.offset(direction))) continue;
                excludeDir |= Dir.get(direction);
            }

            poses.put(pos, excludeDir);
        }

        return poses;
    }
}
