package net.unfamily.colossal_reactors.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

/**
 * Consumable item that fully clears Mekanism radiation from the player when used.
 * Must be held for 3 seconds (like the scanner) before the effect applies.
 * Only effective when Mekanism is loaded; uses reflection to avoid hard dependency.
 */
public class RadiationCureItem extends Item {

    /** Ticks to hold (20 ticks = 1 second). */
    private static final int USE_DURATION_TICKS = 60;

    public RadiationCureItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION_TICKS;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        int totalDuration = getUseDuration(stack, entity);
        int timeUsed = totalDuration - remainingUseTicks;
        if (timeUsed >= USE_DURATION_TICKS && clearPlayerRadiation(player)) {
            stack.shrink(1);
        }
    }

    /**
     * Clears Mekanism radiation from the entity via reflection. Returns true if radiation was cleared.
     */
    public static boolean clearPlayerRadiation(LivingEntity entity) {
        try {
            Class<?> capsClass = Class.forName("mekanism.common.capabilities.Capabilities");
            Object radEntityCap = capsClass.getField("RADIATION_ENTITY").get(null);
            Object handler = entityGetCapability(entity, radEntityCap);
            if (handler == null) {
                return false;
            }
            handler.getClass().getMethod("set", double.class).invoke(handler, 0.0);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Gets entity capability via reflection (single-arg getCapability for Void context). */
    private static Object entityGetCapability(LivingEntity entity, Object capability) {
        for (Method m : entity.getClass().getMethods()) {
            if (!"getCapability".equals(m.getName()) || m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0].isInstance(capability)) {
                try {
                    return m.invoke(entity, capability);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
