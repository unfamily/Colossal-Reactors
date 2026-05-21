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

/** Stacks turbine rods along the clicked rod's axis (one block ahead of {@link TurbineRodBlock#FACING}). */
public class TurbineRodItem extends BlockItem {

    public TurbineRodItem(net.minecraft.world.level.block.Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!clickedState.is(ModBlocks.TURBINE_ROD.get()) || !clickedState.hasProperty(TurbineRodBlock.FACING)) {
            return InteractionResult.PASS;
        }
        Direction axis = clickedState.getValue(TurbineRodBlock.FACING);
        BlockPos placePos = clickedPos.relative(axis);
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
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
}
