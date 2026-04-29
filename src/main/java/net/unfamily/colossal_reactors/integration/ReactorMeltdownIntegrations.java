package net.unfamily.colossal_reactors.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.unfamily.colossal_reactors.Config;

/**
 * Optional integrations when reactor stability hits 0% (meltdown).
 * Each integration is gated by config (evil_things) and by mod presence at runtime (reflection).
 */
public final class ReactorMeltdownIntegrations {

    private static final double REF_VOLUME_ISKA = 65.0 * 65.0 * 65.0; // max scale at 65x65x65
    private static final int ISKA_HORIZONTAL_MAX = 100;
    private static final int ISKA_VERTICAL_MAX = 100;
    private static final int ISKA_TICK_INTERVAL = 10;
    private static final float ISKA_DAMAGE_MAX = 500.0f;

    private ReactorMeltdownIntegrations() {}

    /**
     * Called once when stability drops to 0%. Center is reactor interior center; volume is interior volume.
     */
    public static void triggerMeltdown(ServerLevel level, BlockPos center, int volume, int sizeX, int sizeY, int sizeZ) {
        if (Boolean.TRUE.equals(Config.MEKANISM_RADIATION_INTEGRATION.get())) {
            applyMekanismRadiation(level, center, volume);
        }
        if (Boolean.TRUE.equals(Config.ISKA_UTILS_EXPLOSION_INTEGRATION.get())) {
            triggerIskaExplosion(level, center, volume);
        }
        if (Boolean.TRUE.equals(Config.ALEXS_CAVES_EXPLOSION_INTEGRATION.get())) {
            triggerAlexsCavesExplosion(level, center, sizeX, sizeY, sizeZ);
        }
    }

    /** Mekanism: radiate zone; magnitude scales with reactor size (big reactor = more radiation). */
    private static void applyMekanismRadiation(ServerLevel level, BlockPos center, int volume) {
        try {
            Class<?> apiClass = Class.forName("mekanism.api.radiation.IRadiationManager");
            Object instance = apiClass.getField("INSTANCE").get(null);
            Boolean enabled = (Boolean) apiClass.getMethod("isRadiationEnabled").invoke(instance);
            if (Boolean.FALSE.equals(enabled)) return;
            // Magnitude scales with volume (e.g. 1 Sv per 100 blocks, capped for huge reactors)
            double magnitude = Math.min(10_000.0, Math.max(1.0, volume * 0.01));
            apiClass.getMethod("radiate", net.minecraft.world.level.Level.class, BlockPos.class, double.class)
                    .invoke(instance, level, center, magnitude);
        } catch (Throwable ignored) {
            // Mekanism not present or API changed
        }
    }

    /**
     * Iska Utils: freelag explosion. Max = 100 horizontal, 100 vertical.
     * Scale by reactor volume with a softer curve (exponent 0.5) so smaller reactors do not drop too easily.
     * Reference 65x65x65 = max; below that scale = (volume/ref)^0.5; above that stay at max (avoid TPS lag).
     */
    private static void triggerIskaExplosion(ServerLevel level, BlockPos center, int volume) {
        try {
            Class<?> sysClass = Class.forName("net.unfamily.iskautils.explosion.ExplosionSystem");
            double linear = Math.min(1.0, (double) volume / REF_VOLUME_ISKA);
            double scale = Math.sqrt(linear); // softer curve: smaller reactors keep higher relative scale
            int hRad = Math.max(5, (int) (ISKA_HORIZONTAL_MAX * scale));
            int vRad = Math.max(5, (int) (ISKA_VERTICAL_MAX * scale));
            float damage = (float) Math.max(50.0, ISKA_DAMAGE_MAX * scale);
            sysClass.getMethod("createExplosion", ServerLevel.class, BlockPos.class,
                            int.class, int.class, int.class, float.class, boolean.class)
                    .invoke(null, level, center, hRad, vRad, ISKA_TICK_INTERVAL, damage, false);
        } catch (Throwable ignored) {
            // Iska Utils not present or API changed
        }
    }

    /**
     * Alex's Caves: nuclear explosion. Interior &gt; 10x10x10 uses bomb size (3.0), else nucleeper size (1.0).
     */
    private static void triggerAlexsCavesExplosion(ServerLevel level, BlockPos center, int sizeX, int sizeY, int sizeZ) {
        boolean largeReactor = sizeX > 10 && sizeY > 10 && sizeZ > 10;
        float size = largeReactor ? 3.0f : 1.0f; // bomb vs nucleeper
        try {
            Identifier id = Identifier.fromNamespaceAndPath("alexscaves", "nuclear_explosion");
            EntityType<?> type = level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE)
                    .get(ResourceKey.create(Registries.ENTITY_TYPE, id))
                    .map(net.minecraft.core.Holder::value)
                    .orElse(null);
            if (type == null) return;
            Entity explosion = type.create(level, EntitySpawnReason.TRIGGERED);
            if (explosion == null) return;
            explosion.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            explosion.getClass().getMethod("setSize", float.class).invoke(explosion, size);
            level.addFreshEntity(explosion);
        } catch (Throwable ignored) {
            // Alex's Caves not present or API changed
        }
    }
}
