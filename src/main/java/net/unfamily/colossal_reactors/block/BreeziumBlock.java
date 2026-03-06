package net.unfamily.colossal_reactors.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Breezium (gelid breezium) liquid block with special behaviour:
 * <ul>
 *   <li>Gravity: if the block below is air or flowing breezium (not source), this block "falls" one block.
 *   <li>3x3x3 snow: in a 3x3x3 around this block, any air with a solid block below is replaced by a layer of snow.
 *   <li>Water contact: adjacent water blocks freeze to ice; water source blocks become packed ice.
 * </ul>
 * Cold damage when entities are submerged is handled in {@link net.unfamily.colossal_reactors.fluid.SpecialFluidEffects}.
 */
public class BreeziumBlock extends LiquidBlock {

    public BreeziumBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        scheduleTick(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        scheduleTick(level, pos);
    }

    private void scheduleTick(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide()) return;

        FluidState ourFluid = state.getFluidState();
        if (ourFluid.isEmpty()) return;

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        FluidState belowFluid = belowState.getFluidState();

        // Gravity: if below is air or flowing breezium (not source), fall one block
        boolean belowIsAir = belowState.isAir();
        boolean belowIsFlowingBreezium = !belowFluid.isEmpty()
                && belowFluid.getType() == ourFluid.getType()
                && !belowFluid.isSource();
        if (belowIsAir || belowIsFlowingBreezium) {
            level.setBlock(below, ourFluid.createLegacyBlock(), Block.UPDATE_ALL);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            scheduleTick(level, below);
            return;
        }

        // 3x3x3: place snow on any air that has a solid block below
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolidRender(level, p.below())) {
                        level.setBlock(p, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1), Block.UPDATE_ALL);
                    }
                }
            }
        }

        // Water contact: freeze adjacent water to ice (flowing) or packed ice (source)
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            BlockState adjState = level.getBlockState(adj);
            if (adjState.getFluidState().getType() == Fluids.WATER) {
                boolean isSource = adjState.getFluidState().isSource();
                level.setBlock(adj, (isSource ? Blocks.PACKED_ICE : Blocks.ICE).defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        scheduleTick(level, pos);
    }
}
