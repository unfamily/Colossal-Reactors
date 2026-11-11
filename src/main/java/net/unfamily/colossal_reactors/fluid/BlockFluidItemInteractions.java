package net.unfamily.colossal_reactors.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.unfamily.colossal_reactors.blockentity.HeatingCoilBlockEntity;
import net.unfamily.colossal_reactors.blockentity.MelterBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * World interaction: buckets / fluid items on liquid tanks.
 * Resolves handlers face-agnostic for ports, coil, melter (pipe side-gates unchanged).
 */
public final class BlockFluidItemInteractions {

    private BlockFluidItemInteractions() {}

    public static ItemInteractionResult useItemOnFluidBlock(
            ItemStack stack, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            IFluidHandler probe = stack.getCapability(Capabilities.FluidHandler.ITEM);
            return probe != null
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        IFluidHandler handler = resolveFluidHandler(level, pos, hitResult.getDirection());
        if (handler == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // FluidUtil knows how to swap empty ↔ filled vanilla buckets (manual fill often fails).
        if (FluidUtil.interactWithFluidHandler(player, hand, handler)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    private static IFluidHandler resolveFluidHandler(Level level, BlockPos pos, @Nullable Direction hitFace) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ResourcePortBlockEntity port) {
            // Buckets: raw tank so players can collect in INSERT mode (pipes keep FilteredFluidHandler).
            if (!port.isAllowLiquid()) {
                return null;
            }
            return port.getFluidHandler();
        }
        if (be instanceof HeatingCoilBlockEntity coil) {
            if (!coil.acceptsFluidCapability()) {
                return null;
            }
            return coil.getFluidHandler();
        }
        if (be instanceof MelterBlockEntity melter) {
            return melter.getFluidHandlerForCapability();
        }
        if (be instanceof ReactorBuilderBlockEntity builder) {
            return builder.getFluidTank();
        }
        if (be instanceof TurbineBuilderBlockEntity builder) {
            return builder.getFluidTank();
        }
        IFluidHandler onFace = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, hitFace);
        if (onFace != null) {
            return onFace;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
    }
}
