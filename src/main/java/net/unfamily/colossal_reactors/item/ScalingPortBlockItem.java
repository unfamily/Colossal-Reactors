package net.unfamily.colossal_reactors.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.unfamily.colossal_reactors.multiblock.PortScalingConstants;

import java.util.List;

/** Block item for reactor/turbine ports whose buffer capacity scales with multiblock demand. */
public class ScalingPortBlockItem extends BlockItem {

    public enum Kind {
        REACTOR_POWER_INT,
        REACTOR_POWER_LONG,
        REACTOR_RESOURCE,
        TURBINE_POWER_INT,
        TURBINE_POWER_LONG,
        TURBINE_RESOURCE
    }

    private final Kind kind;

    public ScalingPortBlockItem(Block block, Properties properties, Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        boolean turbine = switch (kind) {
            case TURBINE_POWER_INT, TURBINE_POWER_LONG, TURBINE_RESOURCE -> true;
            default -> false;
        };
        tooltip.add(Component.translatable(
                        turbine ? "tooltip.colossal_reactors.port.scaling.turbine"
                                : "tooltip.colossal_reactors.port.scaling.reactor",
                        PortScalingConstants.DEMAND_MULTIPLIER)
                .withStyle(ChatFormatting.GRAY));
        MutableComponent maxLine = switch (kind) {
            case REACTOR_POWER_INT, TURBINE_POWER_INT -> Component.translatable(
                    "tooltip.colossal_reactors.port.max.energy_int",
                    compact(PortScalingConstants.INT_ENERGY_CAP));
            case REACTOR_POWER_LONG, TURBINE_POWER_LONG -> Component.translatable(
                    "tooltip.colossal_reactors.port.max.energy_long",
                    compact(PortScalingConstants.LONG_ENERGY_CAP));
            case REACTOR_RESOURCE, TURBINE_RESOURCE -> Component.translatable(
                    "tooltip.colossal_reactors.port.max.fluid",
                    compact(PortScalingConstants.MIN_FLUID_TANK_MB));
        };
        tooltip.add(maxLine.withStyle(ChatFormatting.GRAY));
    }

    private static String compact(long value) {
        if (value == Integer.MAX_VALUE) {
            return "2.1B";
        }
        if (value == Long.MAX_VALUE) {
            return "9.22E";
        }
        if (value < 1000) {
            return String.valueOf(value);
        }
        double scaled = value;
        String suffix = "";
        if (scaled >= 1_000_000_000_000L) {
            scaled /= 1_000_000_000_000.0;
            suffix = "T";
        } else if (scaled >= 1_000_000_000L) {
            scaled /= 1_000_000_000.0;
            suffix = "B";
        } else if (scaled >= 1_000_000L) {
            scaled /= 1_000_000.0;
            suffix = "M";
        } else if (scaled >= 1000L) {
            scaled /= 1000.0;
            suffix = "K";
        }
        return scaled == Math.floor(scaled)
                ? String.format("%.0f%s", scaled, suffix)
                : String.format("%.1f%s", scaled, suffix);
    }
}
