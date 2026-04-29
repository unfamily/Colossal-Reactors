package net.unfamily.colossal_reactors.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * World interaction (not GUI): fluid containers and buckets on blocks that expose
 * {@link net.neoforged.neoforge.capabilities.Capabilities.Fluid#BLOCK}.
 */
public final class BlockFluidItemInteractions {

    private BlockFluidItemInteractions() {}

    /**
     * Try bucket / fluid-item transfer into or out of the block's fluid handler at the clicked face.
     * Call from {@link net.minecraft.world.level.block.Block#useItemOn} before falling back to {@link InteractionResult#TRY_WITH_EMPTY_HAND}.
     *
     * @return {@link InteractionResult#PASS} if the stack was empty or no fluid was moved (try GUI / other handlers).
     */
    public static InteractionResult useItemOnFluidBlock(ItemStack stack, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hitResult.getDirection())) {
            player.getInventory().setChanged();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
