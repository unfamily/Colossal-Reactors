package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineRedstonePortBlockEntity extends RedstonePortBlockEntity {

    public TurbineRedstonePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_REDSTONE_PORT_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.colossal_reactors.redstone_port.title");
    }
}
