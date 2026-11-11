package net.unfamily.colossal_reactors.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.unfamily.colossal_reactors.blockentity.HeatingCoilBlockEntity;
import net.unfamily.colossal_reactors.blockentity.MelterBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * World interaction (not GUI): fluid containers and buckets on blocks that expose
 * {@link net.neoforged.neoforge.capabilities.Capabilities.Fluid#BLOCK}.
 * <p>
 * Bucket clicks resolve the block fluid handler <strong>face-agnostic</strong> where the BE
 * owns a tank (ports, coil, melter, builders). Pipe capabilities may still be side-gated.
 */
public final class BlockFluidItemInteractions {

    private BlockFluidItemInteractions() {}

    /**
     * Try bucket / fluid-item transfer into or out of the block's fluid handler.
     * Call from {@link net.minecraft.world.level.block.Block#useItemOn} before falling back to
     * {@link InteractionResult#TRY_WITH_EMPTY_HAND}.
     *
     * @return {@link InteractionResult#PASS} if the stack was empty or no fluid was moved
     */
    public static InteractionResult useItemOnFluidBlock(ItemStack stack, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        // Only claim client success for actual fluid items (avoid stealing non-fluid clicks).
        if (level.isClientSide()) {
            var probe = ItemAccess.forStack(stack.copyWithCount(1)).oneByOne()
                    .getCapability(Capabilities.Fluid.ITEM);
            return probe != null ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        ResourceHandler<FluidResource> handler = resolveFluidHandler(level, pos, hitResult.getDirection());
        if (handler == null) {
            return InteractionResult.PASS;
        }
        try (Transaction tx = Transaction.openRoot()) {
            if (FluidUtil.interactWithFluidHandler(player, hand, pos, handler, tx)) {
                tx.commit();
                player.getInventory().setChanged();
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * Prefer BE-owned tank handlers (ignore click face). Fall back to capability at hit face, then null side.
     */
    @Nullable
    private static ResourceHandler<FluidResource> resolveFluidHandler(Level level, BlockPos pos, @Nullable Direction hitFace) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ResourcePortBlockEntity port) {
            // Buckets: raw tank so players can collect in INSERT mode (pipes keep FilteredFluidHandler).
            if (!port.isAllowLiquid()) {
                return null;
            }
            return port.getFluidResourceHandlerForBucket();
        }
        if (be instanceof HeatingCoilBlockEntity coil) {
            if (!coil.acceptsFluidCapability()) {
                return null;
            }
            return coil.getFluidResourceHandlerForCapability();
        }
        if (be instanceof MelterBlockEntity melter) {
            return melter.getFluidResourceHandlerForCapability();
        }
        if (be instanceof ReactorBuilderBlockEntity builder) {
            return builder.getFluidResourceCapability();
        }
        if (be instanceof TurbineBuilderBlockEntity builder) {
            return builder.getFluidResourceCapability();
        }
        ResourceHandler<FluidResource> onFace = level.getCapability(Capabilities.Fluid.BLOCK, pos, hitFace);
        if (onFace != null) {
            return onFace;
        }
        return level.getCapability(Capabilities.Fluid.BLOCK, pos, null);
    }
}
