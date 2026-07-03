package net.unfamily.colossal_reactors.integration.brandonscore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.blockentity.HighCondPowerPortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;
import net.unfamily.colossal_reactors.blockentity.TurbineHighCondPowerPortBlockEntity;
import net.unfamily.colossal_reactors.transfer.LongBackedEnergyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Brandon's Core OP integration via reflection so core classes load without BC on the classpath.
 */
public final class BrandonScoreIntegration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrandonScoreIntegration.class);
    private static final String OP_STORAGE_CLASS =
            "net.unfamily.colossal_reactors.integration.brandonscore.HighCondPowerPortOpStorage";
    private static final String CAPABILITY_OP_CLASS = "com.brandon3055.brandonscore.capability.CapabilityOP";

    private BrandonScoreIntegration() {}

    public static boolean isBrandonScoreLoaded() {
        return ModList.get().isLoaded("brandonscore");
    }

    public static void registerHighCondPowerPortCapabilities(RegisterCapabilitiesEvent event) {
        registerOpBlockEntity(event, ModBlockEntities.HIGH_COND_POWER_PORT_BE,
                (HighCondPowerPortBlockEntity be, Direction direction) -> be.getOpStorageForCapability());
    }

    public static void registerTurbineHighCondPowerPortCapabilities(RegisterCapabilitiesEvent event) {
        registerOpBlockEntity(event, ModBlockEntities.TURBINE_HIGH_COND_POWER_PORT_BE,
                (TurbineHighCondPowerPortBlockEntity be, Direction direction) -> be.getOpStorageForCapability());
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> void registerOpBlockEntity(
            RegisterCapabilitiesEvent event,
            DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> beType,
            OpProvider<T> provider) {
        if (!ModList.get().isLoaded("brandonscore")) {
            return;
        }
        try {
            Class<?> capClass = Class.forName(CAPABILITY_OP_CLASS);
            Object blockCap = capClass.getField("BLOCK").get(null);
            event.registerBlockEntity((BlockCapability<Object, Direction>) blockCap, beType.get(),
                    (be, direction) -> provider.get(be, direction));
        } catch (Throwable t) {
            logOpDebug("Failed to register OP capability for {}: {}", beType.getId(), t.toString());
        }
    }

    public static Object createOpStorage(LongBackedEnergyHandler storage, long maxExtractPerTick) {
        if (!ModList.get().isLoaded("brandonscore")) {
            return null;
        }
        try {
            Class<?> cls = Class.forName(OP_STORAGE_CLASS);
            return cls.getConstructor(LongBackedEnergyHandler.class, long.class)
                    .newInstance(storage, maxExtractPerTick);
        } catch (Throwable t) {
            logOpDebug("Failed to create OP storage adapter: {}", t.toString());
            return null;
        }
    }

    /**
     * Active OP push into a neighbor using Brandon's Core {@code EnergyUtils.insertEnergy}
     * (requires the neighbor {@link BlockEntity} for sided capability lookup).
     * Deducts the transferred amount from {@code sourceOpStorage} via {@code extractOP}.
     */
    public static long tryPushOpToNeighbor(
            Level level,
            BlockPos neighborPos,
            Direction intoNeighbor,
            Object sourceOpStorage,
            long offer) {
        if (!isBrandonScoreLoaded() || level == null || offer <= 0 || sourceOpStorage == null) {
            return 0L;
        }
        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
        if (neighborBe == null) {
            return 0L;
        }
        try {
            Class<?> energyUtils = Class.forName("com.brandon3055.brandonscore.utils.EnergyUtils");
            long inserted = (long) energyUtils.getMethod(
                            "insertEnergy",
                            BlockEntity.class,
                            long.class,
                            Direction.class,
                            boolean.class)
                    .invoke(null, neighborBe, offer, intoNeighbor, false);
            if (inserted <= 0) {
                logOpDebug("insertEnergy returned 0 at {} face {} for offer {}", neighborPos, intoNeighbor, offer);
                return 0L;
            }
            long extracted = (long) sourceOpStorage.getClass()
                    .getMethod("extractOP", long.class, boolean.class)
                    .invoke(sourceOpStorage, inserted, false);
            if (extracted <= 0) {
                logOpDebug("extractOP returned 0 after insertEnergy {} at {} face {}", inserted, neighborPos, intoNeighbor);
                return 0L;
            }
            return extracted;
        } catch (Throwable t) {
            logOpDebug("OP push failed at {} face {}: {}", neighborPos, intoNeighbor, t.toString());
            return 0L;
        }
    }

    /** @deprecated use {@link #tryPushOpToNeighbor} */
    @Deprecated
    public static long tryPushToNeighbor(Level level, BlockPos neighborPos, Direction intoNeighbor, long offer) {
        return 0L;
    }

    private static void logOpDebug(String message, Object... args) {
        if (Boolean.TRUE.equals(Config.POWER_PORT_OP_DEBUG.get())) {
            LOGGER.info(message, args);
        }
    }

    @FunctionalInterface
    private interface OpProvider<T extends BlockEntity> {
        Object get(T be, Direction direction);
    }
}
