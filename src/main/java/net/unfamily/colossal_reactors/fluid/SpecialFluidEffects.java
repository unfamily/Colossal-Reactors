package net.unfamily.colossal_reactors.fluid;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies special effects when entities are inside our custom fluids:
 * <ul>
 *   <li>Breezium: strong freeze (like iskautils freeze plate but stronger) – set 200 ticks frozen immediately, apply FREEZE damage every tick.</li>
 *   <li>Ender goo: random teleport (source or flowing) with cooldown.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ColossalReactors.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class SpecialFluidEffects {

    /** Like iskautils freeze plate: set directly to 200 so damage (threshold 140) applies immediately. Stronger than plate. */
    private static final int BREEZIUM_FREEZE_TICKS = 200;
    private static final float BREEZIUM_FREEZE_DAMAGE = 2.5F;
    private static final int ENDER_GOO_TELEPORT_COOLDOWN_TICKS = 60;
    private static final int ENDER_GOO_TELEPORT_ATTEMPTS = 16;
    private static final double ENDER_GOO_TELEPORT_RANGE_H = 8.0;
    private static final int ENDER_GOO_TELEPORT_RANGE_V = 8;

    /** Cooldown ticks remaining per entity UUID for ender goo teleport. */
    private static final Map<UUID, Integer> enderGooCooldowns = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide()) return;

        if (entity instanceof LivingEntity living) {
            // Breezium: strong freeze (like iskautils freeze plate – set 200 ticks so damage applies immediately; we also deal damage every tick)
            double breeziumHeight = living.getFluidTypeHeight(ModFluids.GELID_BREEZIUM.getSource().getFluidType());
            if (breeziumHeight <= 0) {
                FluidState atFeet = level.getFluidState(living.blockPosition());
                if (atFeet.getType() == ModFluids.GELID_BREEZIUM.getSource() || atFeet.getType() == ModFluids.GELID_BREEZIUM.getFlowing()) {
                    breeziumHeight = Math.max(breeziumHeight, atFeet.getHeight(level, living.blockPosition()));
                }
            }
            if (breeziumHeight > 0) {
                living.setTicksFrozen(BREEZIUM_FREEZE_TICKS);
                DamageSource freeze = level.damageSources().source(DamageTypes.FREEZE);
                living.hurt(freeze, BREEZIUM_FREEZE_DAMAGE);
            }

            // Ender goo: teleport when inside source or flowing, with cooldown (chorus-fruit style)
            double enderHeight = living.getFluidTypeHeight(ModFluids.ENDER_GOO.getSource().getFluidType());
            if (enderHeight <= 0) {
                FluidState atFeet = level.getFluidState(living.blockPosition());
                if (atFeet.getType() == ModFluids.ENDER_GOO.getSource() || atFeet.getType() == ModFluids.ENDER_GOO.getFlowing()) {
                    enderHeight = Math.max(enderHeight, atFeet.getHeight(level, living.blockPosition()));
                }
            }
            if (enderHeight > 0) {
                int cooldown = enderGooCooldowns.getOrDefault(living.getUUID(), 0);
                if (cooldown <= 0) {
                    tryEnderTeleport(living);
                    enderGooCooldowns.put(living.getUUID(), ENDER_GOO_TELEPORT_COOLDOWN_TICKS);
                } else {
                    int next = cooldown - 1;
                    if (next <= 0) enderGooCooldowns.remove(living.getUUID());
                    else enderGooCooldowns.put(living.getUUID(), next);
                }
            }
        }
    }

    /**
     * Try to teleport the entity to a random position nearby (chorus fruit logic).
     */
    private static void tryEnderTeleport(LivingEntity entity) {
        Level level = entity.level();
        double x0 = entity.getX();
        double y0 = entity.getY();
        double z0 = entity.getZ();

        for (int i = 0; i < ENDER_GOO_TELEPORT_ATTEMPTS; i++) {
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 2.0 * ENDER_GOO_TELEPORT_RANGE_H;
            double y = entity.getY() + (entity.getRandom().nextInt(2 * ENDER_GOO_TELEPORT_RANGE_V + 1) - ENDER_GOO_TELEPORT_RANGE_V);
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 2.0 * ENDER_GOO_TELEPORT_RANGE_H;
            y = Math.clamp(y, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);

            if (entity.isPassenger()) {
                entity.stopRiding();
            }
            if (entity.randomTeleport(x, y, z, true)) {
                level.playSound(null, x0, y0, z0, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                entity.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                return;
            }
        }
    }
}
