package net.unfamily.colossal_reactors.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Pickup for collectible {@link GasBlock}: fluid only for extraction, never insertion. Block removed when drained.
 */
public final class GasFluidInteractions {
    private static final int BUCKET_VOLUME = 1000;

    private GasFluidInteractions() {}

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        GasRegistry.bindBlocks();
        for (RegisteredGas gas : GasRegistry.all()) {
            if (!(gas.block() instanceof GasBlock)) {
                continue;
            }
            event.registerBlock(
                    Capabilities.FluidHandler.BLOCK,
                    (level, pos, state, blockEntity, direction) -> {
                        if (!GasBlock.isCollectable(state)) {
                            return null;
                        }
                        return new GasBlockFluidHandler(gas, level, pos);
                    },
                    gas.block());
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        RegisteredGas gas = GasRegistry.fromState(state);
        if (gas == null || !GasBlock.isCollectable(state)) {
            return;
        }

        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Direction face = event.getFace() != null ? event.getFace() : Direction.UP;

        if (tryExtractOnly(level, player, hand, stack, gas, pos)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static boolean tryExtractOnly(Level level, Player player, InteractionHand hand, ItemStack stack,
                                          RegisteredGas gas, BlockPos pos) {
        if (stack.is(Items.BUCKET)) {
            if (!GasBlock.isCollectableGasAt(level, pos, gas)) {
                return false;
            }
            ItemStack filled = GasBucketItem.createFilledBucket(gas);
            if (filled.isEmpty()) {
                return false;
            }
            player.setItemInHand(hand, filled);
            GasBlock.removeGasBlockIfPresent(level, pos);
            playEmptySound(level, player, gas);
            return true;
        }

        if (stack.isEmpty() || containsPlaceableGas(stack, gas)) {
            return false;
        }

        GasBlockFluidHandler blockHandler = new GasBlockFluidHandler(gas, level, pos);
        IFluidHandlerItem itemHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (itemHandler != null) {
            return extractViaItemHandler(level, player, gas, blockHandler, itemHandler);
        }

        return false;
    }

    private static boolean containsPlaceableGas(ItemStack stack, RegisteredGas gas) {
        return FluidUtil.getFluidContained(stack)
                .filter(fluid -> !fluid.isEmpty() && fluid.getFluid().isSame(gas.sourceFluid()))
                .isPresent();
    }

    private static boolean extractViaItemHandler(Level level, Player player, RegisteredGas gas,
                                                 GasBlockFluidHandler blockHandler, IFluidHandlerItem itemHandler) {
        if (itemHandler.getTanks() == 0) {
            return false;
        }
        if (!itemHandler.getFluidInTank(0).isEmpty()) {
            return false;
        }
        FluidStack available = blockHandler.drain(BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) {
            return false;
        }
        FluidStack toMove = available.copy();
        toMove.setAmount(Math.min(available.getAmount(), itemHandler.getTankCapacity(0)));
        if (!itemHandler.isFluidValid(0, toMove)) {
            return false;
        }
        int filled = itemHandler.fill(toMove, IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0) {
            return false;
        }
        blockHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        playEmptySound(level, player, gas);
        return true;
    }

    private static void removeGasBlock(Level level, BlockPos pos) {
        GasBlock.removeGasBlockIfPresent(level, pos);
    }

    private static void playEmptySound(Level level, Player player, RegisteredGas gas) {
        var soundEvent = gas.sourceFluid().getFluidType().getSound(SoundActions.BUCKET_EMPTY);
        if (soundEvent != null) {
            level.playSound(null, player.blockPosition(), soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    static final class GasBlockFluidHandler implements IFluidHandler {
        private final RegisteredGas gas;
        private final Level level;
        private final BlockPos pos;

        GasBlockFluidHandler(RegisteredGas gas, Level level, BlockPos pos) {
            this.gas = gas;
            this.level = level;
            this.pos = pos;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return new FluidStack(gas.sourceFluid(), BUCKET_VOLUME);
        }

        @Override
        public int getTankCapacity(int tank) {
            return BUCKET_VOLUME;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!resource.isEmpty() && !resource.is(gas.sourceFluid())) {
                return FluidStack.EMPTY;
            }
            return drain(resource.isEmpty() ? BUCKET_VOLUME : resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !GasBlock.isCollectableGasAt(level, pos, gas)) {
                return FluidStack.EMPTY;
            }
            int amount = Math.min(maxDrain, BUCKET_VOLUME);
            FluidStack drained = new FluidStack(gas.sourceFluid(), amount);
            if (action == FluidAction.EXECUTE && level instanceof ServerLevel) {
                removeGasBlock(level, pos);
            }
            return drained;
        }
    }
}
