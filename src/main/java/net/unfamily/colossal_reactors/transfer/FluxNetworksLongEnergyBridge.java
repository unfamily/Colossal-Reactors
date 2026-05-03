package net.unfamily.colossal_reactors.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Optional push to Flux Networks {@code IFNEnergyStorage} ({@code fn_energy}) in {@code long} units when that mod is loaded.
 * Standard {@link net.neoforged.neoforge.transfer.energy.EnergyHandler} path follows in {@link net.unfamily.colossal_reactors.blockentity.PowerPortBlockEntity#tick()}.
 */
public final class FluxNetworksLongEnergyBridge {

    private static final String FLUX_MOD_ID = "fluxnetworks";
    private static final Object INIT_LOCK = new Object();

    private static volatile boolean resolved;
    @Nullable
    private static Object fluxBlockCapability;
    @Nullable
    private static Method receiveEnergyL;
    @Nullable
    private static Method canReceive;

    private FluxNetworksLongEnergyBridge() {}

    /**
     * Attempts to insert up to {@code maxReceive} RF into the neighbor via Flux long API.
     *
     * @return amount accepted by the neighbor (caller should remove this from its own storage)
     */
    public static long tryReceiveEnergyLong(Level level, BlockPos neighborPos, Direction neighborInsertFace, long maxReceive) {
        if (maxReceive <= 0 || level == null || level.isClientSide()) {
            return 0L;
        }
        resolve();
        if (!(fluxBlockCapability instanceof BlockCapability<?, ?> cap) || receiveEnergyL == null || canReceive == null) {
            return 0L;
        }
        try {
            @SuppressWarnings("unchecked")
            Object sink = level.getCapability((BlockCapability<Object, Direction>) cap, neighborPos, neighborInsertFace);
            if (sink == null) return 0L;
            if (!(boolean) canReceive.invoke(sink)) return 0L;
            Object moved = receiveEnergyL.invoke(sink, maxReceive, Boolean.FALSE);
            if (!(moved instanceof Number n)) return 0L;
            long v = n.longValue();
            return v > 0 ? v : 0L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static void resolve() {
        if (resolved) return;
        synchronized (INIT_LOCK) {
            if (resolved) return;
            resolved = true;
            fluxBlockCapability = Boolean.FALSE;
            try {
                if (!ModList.get().isLoaded(FLUX_MOD_ID)) return;
                Class<?> fluxCaps = Class.forName("sonar.fluxnetworks.api.FluxCapabilities");
                fluxBlockCapability = fluxCaps.getField("BLOCK").get(null);
                Class<?> fnIface = Class.forName("sonar.fluxnetworks.api.energy.IFNEnergyStorage");
                receiveEnergyL = fnIface.getMethod("receiveEnergyL", long.class, boolean.class);
                canReceive = fnIface.getMethod("canReceive");
            } catch (Throwable ignored) {
                fluxBlockCapability = Boolean.FALSE;
            }
        }
    }
}
