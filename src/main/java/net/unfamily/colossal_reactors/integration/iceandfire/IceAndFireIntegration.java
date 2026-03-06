package net.unfamily.colossal_reactors.integration.iceandfire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.LightningGeneratorBlockEntity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Optional integration with Ice and Fire (CE). When the mod is present, registers a handler for
 * ON_DRAGON_DAMAGE_BLOCK: when a lightning dragon's breath/charge hits the high-power lightning rod
 * or the lightning generator block, the generator receives RF (config: lightning_generator.050_dragon_rf).
 * Same tier/effect as the dragon forge: the dragon must hit this block (e.g. by aiming when ridden,
 * or when the breath impact happens to land on the rod). Uses reflection so Ice and Fire is not
 * required at compile time.
 */
public final class IceAndFireIntegration {

    public static void register() {
        try {
            Class<?> eventClass = Class.forName("com.iafenvoy.iceandfire.event.IafEvents");
            Object event = eventClass.getField("ON_DRAGON_DAMAGE_BLOCK").get(null);
            Class<?> callbackClass = Class.forName("com.iafenvoy.iceandfire.event.IafEvents$DragonFireDamageWorld");
            Object handler = Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[]{callbackClass},
                    new DamageBlockHandler()
            );
            for (Method m : event.getClass().getMethods()) {
                if ("register".equals(m.getName()) && m.getParameterCount() == 1) {
                    m.invoke(event, handler);
                    break;
                }
            }
        } catch (Throwable t) {
            net.unfamily.colossal_reactors.ColossalReactors.LOGGER.warn("Failed to register Ice and Fire dragon damage handler: {}", t.getMessage());
        }
    }

    private static final class DamageBlockHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!"onDamageBlock".equals(method.getName()) || args == null || args.length < 4) {
                return false;
            }
            Object dragon = args[0];
            double x = ((Number) args[1]).doubleValue();
            double y = ((Number) args[2]).doubleValue();
            double z = ((Number) args[3]).doubleValue();

            Class<?> dragonTypeClass = Class.forName("com.iafenvoy.iceandfire.data.DragonType");
            Object lightningType = Class.forName("com.iafenvoy.iceandfire.registry.IafDragonTypes")
                    .getField("LIGHTNING").get(null);
            Object dragonType = dragon.getClass().getField("dragonType").get(dragon);
            if (dragonType != lightningType) return false;

            Object world = dragon.getClass().getMethod("getWorld").invoke(dragon);
            Level level = (Level) world;
            BlockPos center = BlockPos.containing((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));

            BlockPos generatorPos = null;
            if (level.getBlockState(center).is(ModBlocks.HIGH_POWER_LIGHTNING_ROD.get())) {
                generatorPos = center.below();
            } else if (level.getBlockState(center).is(ModBlocks.LIGHTNING_GENERATOR.get())) {
                generatorPos = center;
            }
            if (generatorPos == null) return false;

            var be = level.getBlockEntity(generatorPos);
            if (!(be instanceof LightningGeneratorBlockEntity generator)) return false;

            int rf = Config.LIGHTNING_GENERATOR_DRAGON_RF.get();
            int added = generator.receiveDragonStrikeEnergy(rf);
            if (added > 0) be.setChanged();
            return true;
        }
    }
}
