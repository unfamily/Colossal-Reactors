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
 * Max units per rod = URANIUM_INGOT_MB (default 1000). Block state FILL (0-12) selects model by fill percentage.
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

    /** Maps fill percentage to block state level 0-12 for model selection (0%, 5%, 10%, 20%, ..., 95%, 100%). */
    private static int fillPercentToLevel(float fillPercent) {
        if (fillPercent <= 0f) return 0;
        if (fillPercent >= 1f) return 12;
        int p = (int) (fillPercent * 100);
        if (p <= 5) return 1;
        if (p <= 10) return 2;
        if (p <= 20) return 3;
        if (p <= 30) return 4;
        if (p <= 40) return 5;
        if (p <= 50) return 6;
        if (p <= 60) return 7;
        if (p <= 70) return 8;
        if (p <= 80) return 9;
        if (p <= 90) return 10;
        return 11; // 91-99%
    }

    /** Updates block state FILL so client uses the correct percentage model. */
    private void updateBlockState() {
        if (level == null) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof ReactorRodBlock)) return;
        int fillLevel = fillPercentToLevel(getFillLevel());
        if (state.getValue(ReactorRodBlock.FILL) != fillLevel) {
            level.setBlock(worldPosition, state.setValue(ReactorRodBlock.FILL, fillLevel), Block.UPDATE_CLIENTS);
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
