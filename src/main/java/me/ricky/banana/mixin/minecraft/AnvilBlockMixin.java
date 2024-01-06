package me.ricky.banana.mixin.minecraft;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilBlock.class)
public abstract class AnvilBlockMixin {
    @Unique private static final VoxelShape BASE_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    @Unique private static final VoxelShape X_STEP_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 2.0D, 16.0D, 4.0D, 14.0D);
    @Unique private static final VoxelShape X_STEM_SHAPE = Block.createCuboidShape(0.0D, 5.0D, 2.0D, 16.0D, 10.0D, 14.0D);
    @Unique private static final VoxelShape X_FACE_SHAPE = Block.createCuboidShape(0.0D, 10.0D, 2.0D, 16.0D, 16.0D, 14.0D);
    @Unique private static final VoxelShape Z_STEP_SHAPE = Block.createCuboidShape(2.0D, 0.0D, 0.0D, 14.0D, 4.0D, 16.0D);
    @Unique private static final VoxelShape Z_STEM_SHAPE = Block.createCuboidShape(2.0D, 5.0D, 0.0D, 14.0D, 10.0D, 16.0D);
    @Unique private static final VoxelShape Z_FACE_SHAPE = Block.createCuboidShape(2.0D, 10.0D, 0.0D, 14.0D, 16.0D, 16.0D);
    @Unique private static final VoxelShape X_AXIS_SHAPE = VoxelShapes.union(BASE_SHAPE, X_STEP_SHAPE, X_STEM_SHAPE, X_FACE_SHAPE);
    @Unique private static final VoxelShape Z_AXIS_SHAPE = VoxelShapes.union(BASE_SHAPE, Z_STEP_SHAPE, Z_STEM_SHAPE, Z_FACE_SHAPE);

    @Inject(method = "getOutlineShape", at = @At("HEAD"), cancellable = true)
    public void getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        Direction direction = state.get(HorizontalFacingBlock.FACING);
        cir.setReturnValue(direction.getAxis() == Direction.Axis.X ? X_AXIS_SHAPE : Z_AXIS_SHAPE);
    }
}
