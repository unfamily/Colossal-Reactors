package net.unfamily.colossal_reactors.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineControllerBlock;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;
import org.jetbrains.annotations.Nullable;

/**
 * Normal block placement on any face; when clicking an existing rod, places at the first free cell
 * along {@link TurbineRodBlock#FACING}, skipping over consecutive rods on that axis (scaffold-style).
 */
public class TurbineRodItem extends BlockItem {

    /** Max rod segments to walk when extending a stack (matches turbine size cap). */
    private static final int ROD_STACK_SCAN_CAP = 65;

    public TurbineRodItem(net.minecraft.world.level.block.Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!clickedState.is(ModBlocks.TURBINE_ROD.get()) || !clickedState.hasProperty(TurbineRodBlock.FACING)) {
            return super.useOn(context);
        }
        Direction axis = clickedState.getValue(TurbineRodBlock.FACING);
        BlockPos placePos = findRodStackTip(level, clickedPos, axis);
        if (placePos == null) {
            return super.useOn(context);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockState newRod = ModBlocks.TURBINE_ROD.get().defaultBlockState()
                .setValue(TurbineRodBlock.FACING, axis);
        if (level.setBlock(placePos, newRod, Block.UPDATE_ALL)) {
            TurbineControllerBlock.notifyTurbineStructureChanged(level, placePos);
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    /**
     * First replaceable cell along {@code axis} after {@code clickedRod}, skipping rods that share the same facing.
     */
    @Nullable
    private static BlockPos findRodStackTip(Level level, BlockPos clickedRod, Direction axis) {
        BlockPos cursor = clickedRod;
        for (int step = 0; step < ROD_STACK_SCAN_CAP; step++) {
            BlockPos next = cursor.relative(axis);
            BlockState nextState = level.getBlockState(next);
            if (isRodAlongAxis(nextState, axis)) {
                cursor = next;
                continue;
            }
            if (nextState.canBeReplaced()) {
                return next;
            }
            return null;
        }
        return null;
    }

    private static boolean isRodAlongAxis(BlockState state, Direction axis) {
        return state.is(ModBlocks.TURBINE_ROD.get())
                && state.hasProperty(TurbineRodBlock.FACING)
                && state.getValue(TurbineRodBlock.FACING) == axis;
    }
}
