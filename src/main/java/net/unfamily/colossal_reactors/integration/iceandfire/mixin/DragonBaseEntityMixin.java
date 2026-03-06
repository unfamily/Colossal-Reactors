package net.unfamily.colossal_reactors.integration.iceandfire.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.LightningGeneratorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/**
 * Makes the high-power lightning rod + generator a valid burn target for lightning dragons,
 * identical in behaviour to DragonForgeInputBlockEntity.
 *
 * All Minecraft API calls use NeoForge (MojMaps) names directly via a cast to Mob,
 * avoiding reflection mismatches caused by Yarn→MojMaps remapping in the IAF jar.
 *
 * IAF-defined methods (getDragonStage, canPositionBeSeen, breathFireAtPos, setBreathingFire)
 * keep their source names. Note that breathFireAtPos is protected, so we must traverse the
 * class hierarchy with getDeclaredMethod + setAccessible to invoke it.
 */
@Mixin(targets = "com.iafenvoy.iceandfire.entity.DragonBaseEntity")
public abstract class DragonBaseEntityMixin {

    /**
     * At the head of updateBurnTarget(): if the burningTarget is our high-power rod above a
     * LightningGeneratorBlockEntity, handle breathing ourselves and cancel the vanilla IAF path
     * (which would otherwise clear burningTarget because it is not a DragonForgeInputBlockEntity).
     */
    @Inject(method = "updateBurnTarget", at = @At("HEAD"), cancellable = true, remap = false)
    private void colossalReactors$acceptRodTarget(CallbackInfo ci) {
        try {
            Object burningTarget = this.getClass().getField("burningTarget").get(this);
            if (burningTarget == null) return;

            // Filter: only tamed lightning dragons
            Object dragonType = this.getClass().getField("dragonType").get(this);
            Object lightningType = Class.forName("com.iafenvoy.iceandfire.registry.IafDragonTypes")
                    .getField("LIGHTNING").get(null);
            if (!java.util.Objects.equals(dragonType, lightningType)) return;

            // Cast to NeoForge Mob - DragonBaseEntity → TameableEntity → Mob on NeoForge
            Mob mob = (Mob) (Object) this;
            Level level = mob.level();
            if (level.isClientSide()) return;

            int tx = ((Number) burningTarget.getClass().getMethod("getX").invoke(burningTarget)).intValue();
            int ty = ((Number) burningTarget.getClass().getMethod("getY").invoke(burningTarget)).intValue();
            int tz = ((Number) burningTarget.getClass().getMethod("getZ").invoke(burningTarget)).intValue();
            BlockPos pos = new BlockPos(tx, ty, tz);

            // Only accept our high-power rod with a generator directly below
            if (!level.getBlockState(pos).is(ModBlocks.HIGH_POWER_LIGHTNING_ROD.get())) return;
            if (!(level.getBlockEntity(pos.below()) instanceof LightningGeneratorBlockEntity)) return;

            // Distance check: IAF uses squaredDistanceTo < (115 * stage), not (115*stage)²
            // distanceToSqr is the NeoForge (MojMaps) name for squaredDistanceTo
            float maxDist = 115F * ((Number) this.getClass().getMethod("getDragonStage").invoke(this)).floatValue();
            double cx = tx + 0.5, cy = ty + 0.5, cz = tz + 0.5;
            if (mob.distanceToSqr(cx, cy, cz) >= maxDist) return;

            // canPositionBeSeen is IAF-defined so its name does not get remapped
            if (!(Boolean) this.getClass()
                    .getMethod("canPositionBeSeen", double.class, double.class, double.class)
                    .invoke(this, cx, cy, cz)) return;

            // Look at rod
            // setLookAt is the NeoForge (MojMaps) name for LookControl.lookAt
            mob.getLookControl().setLookAt(cx, cy, cz, 180F, 180F);

            // breathFireAtPos is *protected* so getMethod() (public-only) would fail.
            // We must traverse the class hierarchy using getDeclaredMethod + setAccessible.
            Method breathMethod = findDeclaredMethod(this.getClass(), "breathFireAtPos", BlockPos.class);
            if (breathMethod != null) {
                breathMethod.setAccessible(true);
                breathMethod.invoke(this, pos);
            } else {
                // Fallback: at least mark breathing so fireBreathTicks accumulates
                this.getClass().getMethod("setBreathingFire", boolean.class).invoke(this, true);
            }

            // Notify all clients so they render breath particles aimed at the rod
            try {
                Class<?> payloadClass = Class.forName(
                        "com.iafenvoy.iceandfire.network.payload.DragonSetBurnBlockS2CPayload");
                Object payload = payloadClass
                        .getConstructor(int.class, boolean.class, BlockPos.class)
                        .newInstance(mob.getId(), true, pos);
                Class<?> helper = Class.forName("com.iafenvoy.uranus.ServerHelper");
                for (Method m : helper.getMethods())
                    if ("sendToAll".equals(m.getName()) && m.getParameterCount() == 1) {
                        m.invoke(null, payload);
                        break;
                    }
            } catch (Throwable t2) {
                ColossalReactors.LOGGER.trace("DragonBaseEntityMixin S2C: {}", t2.getMessage());
            }

            // Cancel: keep burningTarget set so the dragon keeps breathing next tick.
            ci.cancel();
        } catch (Throwable t) {
            ColossalReactors.LOGGER.trace("DragonBaseEntityMixin: {}", t.getMessage());
        }
    }

    /**
     * Make isFuelingForge() return true when the dragon targets our rod+generator.
     * This prevents DragonAIWanderGoal (which checks isFuelingForge) from interrupting
     * the dragon while it is breathing at the high-power rod.
     */
    @Inject(method = "isFuelingForge", at = @At("RETURN"), cancellable = true, remap = false)
    private void colossalReactors$rodCountsAsForge(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) return;
        try {
            Object burningTarget = this.getClass().getField("burningTarget").get(this);
            if (burningTarget == null) return;
            int tx = ((Number) burningTarget.getClass().getMethod("getX").invoke(burningTarget)).intValue();
            int ty = ((Number) burningTarget.getClass().getMethod("getY").invoke(burningTarget)).intValue();
            int tz = ((Number) burningTarget.getClass().getMethod("getZ").invoke(burningTarget)).intValue();
            Mob mob = (Mob) (Object) this;
            BlockPos pos = new BlockPos(tx, ty, tz);
            if (mob.level().getBlockState(pos).is(ModBlocks.HIGH_POWER_LIGHTNING_ROD.get())
                    && mob.level().getBlockEntity(pos.below()) instanceof LightningGeneratorBlockEntity) {
                cir.setReturnValue(true);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Traverse the class hierarchy to find a declared method (including protected/private)
     * with the given name and parameter types. Returns null if not found.
     */
    private static Method findDeclaredMethod(Class<?> clazz, String name, Class<?>... params) {
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}
