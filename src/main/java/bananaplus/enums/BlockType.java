package bananaplus.enums;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public enum BlockType {
    // Only unbreakable blocks like bedrock or barriers
    Hardness,
    // Blocks that don't break in an explosion
    Resistance,
    // Any block that would prevent explosion damage. even if it could break
    NotEmpty;

    public boolean isResistant(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return switch (this) {
            case Hardness -> state.getBlock().getHardness() < 0;
            case Resistance -> state.getBlock().getBlastResistance() >= 600;
            case NotEmpty -> !state.isReplaceable();
        };
    }
}
