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
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * BlockEntity for Power Port. Large energy buffer; reactor pushes in via {@link #receiveEnergyFromReactor(int)}.
 * Each tick the port pushes energy out to adjacent blocks that can receive (cables, machines).
 * Capacity and max extract per tick are read from Config (ports.power).
 */
public class PowerPortBlockEntity extends BlockEntity {

    private int getMaxExtractPerTick() {
        return net.unfamily.colossal_reactors.Config.POWER_PORT_MAX_EXTRACT.get();
    }

    private final SimpleEnergyHandler core;
    /** External automation: insert blocked; extract allowed (same behavior as legacy OutputOnlyEnergyWrapper). */
    private final EnergyHandler capabilityView;

    public PowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_PORT_BE.get(), pos, state);
        int capacity = net.unfamily.colossal_reactors.Config.POWER_PORT_CAPACITY.get();
        int maxExtract = net.unfamily.colossal_reactors.Config.POWER_PORT_MAX_EXTRACT.get();
        this.core = new SimpleEnergyHandler(capacity, 0, maxExtract, 0) {
            @Override
            protected void onEnergyChanged(int previousAmount) {
                setChanged();
            }
        };
        this.capabilityView = new LimitingEnergyHandler(core, 0, Integer.MAX_VALUE);
    }

    /**
     * Server tick: push energy to adjacent blocks that can receive (cables, machines).
     */
    public void tick() {
        if (level == null || level.isClientSide()) return;
        int budget = Math.min(getMaxExtractPerTick(), (int) core.getAmountAsLong());
        if (budget <= 0) return;
        for (Direction direction : Direction.values()) {
            if (budget <= 0) break;
            BlockPos neighborPos = worldPosition.relative(direction);
            EnergyHandler neighbor = level.getCapability(Capabilities.Energy.BLOCK, neighborPos, direction.getOpposite());
            if (neighbor == null) continue;
            int moved = EnergyHandlerUtil.move(core, neighbor, budget, null);
            if (moved > 0) {
                budget -= moved;
                setChanged();
            }
        }
    }

    /** Internal storage (legacy callers); prefer {@link #getEnergyCore()} for RF-style reads. */
    public EnergyHandler getEnergyStorage() {
        return core;
    }

    public EnergyHandler getEnergyCore() {
        return core;
    }

    /** Output-only view for capability: cables extract; reactor uses {@link #receiveEnergyFromReactor(int)}. */
    public EnergyHandler getEnergyHandlerForCapability() {
        return capabilityView;
    }

    /**
     * Called by the reactor controller to push produced energy into this port.
     *
     * @param maxAmount max RF to accept this call
     * @return amount actually accepted
     */
    public int receiveEnergyFromReactor(int maxAmount) {
        if (maxAmount <= 0) return 0;
        long space = core.getCapacityAsLong() - core.getAmountAsLong();
        int toAdd = (int) Math.min(maxAmount, space);
        if (toAdd <= 0) return 0;
        core.set((int) (core.getAmountAsLong() + toAdd));
        return toAdd;
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
