package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.multiblock.PortScalingConstants;

public class TurbineResourcePortBlockEntity extends ResourcePortBlockEntity {

    public TurbineResourcePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_RESOURCE_PORT_BE.get(), pos, state);
        setPortMedium(PortMedium.LIQUID);
    }

    @Override
    protected boolean isTurbineResourcePort() {
        return true;
    }

    @Override
    protected long tankCapacityMb() {
        return PortScalingConstants.MIN_FLUID_TANK_MB;
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (getPortMedium() == PortMedium.SOLID) {
            setPortMedium(PortMedium.LIQUID);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.colossal_reactors.resource_port.title");
    }
}
