package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ReactorRodBlock;

/**
 * BlockEntity for reactor rod. Holds fuel in "units" (same scale as config URANIUM_INGOT_MB: 1 ingot = 1000 units).
 * Max units per rod = URANIUM_INGOT_MB (default 1000). Block state FULL drives full vs empty model.
 */
public class ReactorRodBlockEntity extends BlockEntity {

    private static final String TAG_FUEL_UNITS = "fuelUnits";

    private int fuelUnits = 0;

    public ReactorRodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_ROD_BE.get(), pos, state);
    }

    public int getFuelUnits() {
        return fuelUnits;
    }

    public void setFuelUnits(int units) {
        this.fuelUnits = Math.max(0, Math.min(units, getMaxFuelUnits()));
        setChanged();
        updateBlockState();
    }

    /** Updates block state FULL so client uses full or empty model. */
    private void updateBlockState() {
        if (level == null) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof ReactorRodBlock)) return;
        boolean full = fuelUnits >= getMaxFuelUnits();
        if (state.getValue(ReactorRodBlock.FULL) != full) {
            level.setBlock(worldPosition, state.setValue(ReactorRodBlock.FULL, full), Block.UPDATE_CLIENTS);
        }
    }

    /** Max units per rod (from config, same value as "MB" per uranium ingot). */
    public static int getMaxFuelUnits() {
        return Config.URANIUM_INGOT_MB.get();
    }

    /** Fill level 0..1 for rendering (1 = full = opaque). */
    public float getFillLevel() {
        int max = getMaxFuelUnits();
        return max <= 0 ? 0f : (float) fuelUnits / max;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_FUEL_UNITS, fuelUnits);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuelUnits = tag.getInt(TAG_FUEL_UNITS);
        updateBlockState();
    }
}
