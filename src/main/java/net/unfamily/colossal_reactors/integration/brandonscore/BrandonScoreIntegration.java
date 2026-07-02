package net.unfamily.colossal_reactors.integration.brandonscore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.unfamily.colossal_reactors.blockentity.HighCondPowerPortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;
import net.unfamily.colossal_reactors.blockentity.TurbineHighCondPowerPortBlockEntity;
import net.unfamily.colossal_reactors.transfer.LongBackedEnergyHandler;

/**
 * Optional Brandon's Core OP integration via reflection so core classes load without BC on the classpath.
 */
public final class BrandonScoreIntegration {

    private static final String OP_STORAGE_CLASS =
            "net.unfamily.colossal_reactors.integration.brandonscore.HighCondPowerPortOpStorage";
    private static final String CAPABILITY_OP_CLASS = "com.brandon3055.brandonscore.capability.CapabilityOP";

    private BrandonScoreIntegration() {}

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
        } catch (Throwable ignored) {
            // Brandon's Core not present or API changed
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
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static long tryPushToNeighbor(Level level, BlockPos neighborPos, Direction intoNeighbor, long offer) {
        if (!ModList.get().isLoaded("brandonscore") || level == null || offer <= 0) {
            return 0L;
        }
        try {
            Class<?> capClass = Class.forName(CAPABILITY_OP_CLASS);
            Object blockCap = capClass.getField("BLOCK").get(null);
            Object nativeOp = level.getCapability((BlockCapability<Object, Direction>) blockCap, neighborPos, intoNeighbor);
            if (nativeOp == null) {
                return 0L;
            }
            if (!(boolean) nativeOp.getClass().getMethod("canReceive").invoke(nativeOp)) {
                return 0L;
            }
            return (long) nativeOp.getClass().getMethod("receiveOP", long.class, boolean.class)
                    .invoke(nativeOp, offer, false);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    @FunctionalInterface
    private interface OpProvider<T extends BlockEntity> {
        Object get(T be, Direction direction);
    }
}
