package net.unfamily.colossal_reactors.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.turbine.TurbineBladePlacement;

/** Places turbine blades on rods with balanced ring growth (not standard block placement). */
public class TurbineBladeItem extends BlockItem {

    public TurbineBladeItem(net.minecraft.world.level.block.Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.TURBINE_ROD.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (TurbineBladePlacement.placeNextBlade(level, pos, state)) {
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
}
