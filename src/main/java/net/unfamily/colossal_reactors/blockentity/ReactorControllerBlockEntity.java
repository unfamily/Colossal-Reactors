package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.reactor.ReactorValidation;

/**
 * BlockEntity for the reactor controller. Caches multiblock validation and re-validates periodically.
 */
public class ReactorControllerBlockEntity extends BlockEntity {

    private int ticksSinceValidation = Integer.MAX_VALUE; // trigger validation on first tick
    private ReactorValidation.Result cachedResult;

    public ReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CONTROLLER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReactorControllerBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        int interval = Config.REACTOR_VALIDATION_INTERVAL_TICKS.get();
        be.ticksSinceValidation++;
        if (be.ticksSinceValidation >= interval) {
            be.ticksSinceValidation = 0;
            if (Boolean.TRUE.equals(Config.REACTOR_VALIDATION_DEBUG.get())) {
                ColossalReactors.LOGGER.info("[ReactorController] Tick running validation at controller pos={} (debug enabled)", pos);
            }
            Direction back = state.getValue(net.unfamily.colossal_reactors.block.ReactorControllerBlock.FACING).getOpposite();
            BlockPos startPos = pos.relative(back);
            be.cachedResult = ReactorValidation.validate(level, startPos, back);
            be.setChanged();
        }
    }

    public ReactorValidation.Result getCachedResult() {
        return cachedResult;
    }

    /** Set from block when validation is run (e.g. on player click). */
    public void setCachedResult(ReactorValidation.Result result) {
        this.cachedResult = result;
    }

    public void invalidateCache() {
        cachedResult = null;
        if (level != null && !level.isClientSide()) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (cachedResult != null && cachedResult.valid()) {
            tag.putInt("val_minX", cachedResult.minX());
            tag.putInt("val_minY", cachedResult.minY());
            tag.putInt("val_minZ", cachedResult.minZ());
            tag.putInt("val_maxX", cachedResult.maxX());
            tag.putInt("val_maxY", cachedResult.maxY());
            tag.putInt("val_maxZ", cachedResult.maxZ());
            tag.putInt("val_rodCount", cachedResult.rodCount());
            tag.putInt("val_rodColumns", cachedResult.rodColumns());
            tag.putInt("val_coolantCount", cachedResult.coolantCount());
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("val_minX")) {
            cachedResult = new ReactorValidation.Result(
                    true,
                    tag.getInt("val_minX"), tag.getInt("val_minY"), tag.getInt("val_minZ"),
                    tag.getInt("val_maxX"), tag.getInt("val_maxY"), tag.getInt("val_maxZ"),
                    tag.getInt("val_rodCount"), tag.getInt("val_rodColumns"), tag.getInt("val_coolantCount")
            );
        } else {
            cachedResult = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
