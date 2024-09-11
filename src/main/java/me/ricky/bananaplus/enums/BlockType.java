package me.ricky.bananaplus.enums;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public enum BlockType {
    // Only unbreakable blocks like bedrock or barriers
    Hardness,
    // Blocks that don't break in an explosion
    Resistance,
    // Any block that would prevent explosion damage once
    NotEmpty;

    public boolean resists(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return resists(state);
    }

    public boolean resists(BlockState state) {
        return switch (this) {
            case Hardness -> state.getBlock().getHardness() < 0;
            case Resistance -> state.getBlock().getBlastResistance() >= 600;
            case NotEmpty -> !state.isReplaceable();
        };
    }
}