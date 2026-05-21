package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.Config;

public class TurbineResourcePortBlockEntity extends ResourcePortBlockEntity {

    public TurbineResourcePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_RESOURCE_PORT_BE.get(), pos, state);
    }

    @Override
    protected int tankCapacityMb() {
        return Config.TURBINE_RESOURCE_PORT_TANK_CAPACITY_MB.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.turbine_resource_port");
    }
}
