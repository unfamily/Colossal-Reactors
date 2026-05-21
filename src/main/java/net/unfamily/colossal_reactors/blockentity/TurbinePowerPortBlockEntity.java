package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.LimitingEnergyHandler;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.transfer.IntBackedEnergyHandler;

public class TurbinePowerPortBlockEntity extends BlockEntity implements TurbinePowerPort {

    private final int maxExtractPerTick;
    private final IntBackedEnergyHandler core;
    private final EnergyHandler capabilityView;

    public TurbinePowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_POWER_PORT_BE.get(), pos, state);
        int capacity = Config.TURBINE_POWER_PORT_CAPACITY.get();
        int maxExtractCfg = Config.TURBINE_POWER_PORT_MAX_EXTRACT.get();
        this.maxExtractPerTick = Math.min(capacity, maxExtractCfg);
        this.core = new IntBackedEnergyHandler(capacity, 0, capacity, 0, this::setChanged);
        this.capabilityView = new LimitingEnergyHandler(core, 0, Integer.MAX_VALUE);
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        int budget = Math.min(maxExtractPerTick, core.getEnergyStored());
        if (budget <= 0) return;
        for (Direction direction : Direction.values()) {
            if (budget <= 0) break;
            BlockPos neighborPos = worldPosition.relative(direction);
            Direction intoNeighbor = direction.getOpposite();
            EnergyHandler neighbor = level.getCapability(Capabilities.Energy.BLOCK, neighborPos, intoNeighbor);
            if (neighbor == null) continue;
            int moved = EnergyHandlerUtil.move(core, neighbor, budget, null);
            if (moved > 0) {
                budget -= moved;
                setChanged();
            }
        }
    }

    public EnergyHandler getEnergyHandlerForCapability() {
        return capabilityView;
    }

    @Override
    public long receiveEnergyFromTurbine(long maxAmount) {
        return core.addEnergyFromReactor(maxAmount);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        core.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        core.deserialize(input);
    }
}
