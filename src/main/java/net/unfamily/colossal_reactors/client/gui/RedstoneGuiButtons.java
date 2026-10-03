package net.unfamily.colossal_reactors.client.gui;

import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.RedstoneMode;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla-button redstone mode control (iskandert_utilities MachineGuiButtons style).
 */
public final class RedstoneGuiButtons {
    public static final int ICON_SIZE = 16;

    public static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/redstone_gui.png");

    private RedstoneGuiButtons() {}

    public static int displayMode(int mode, boolean allowPulse) {
        if (mode == RedstoneMode.PULSE.getId() && !allowPulse) {
            return RedstoneMode.DISABLED.getId();
        }
        return mode;
    }

    public static ItemStack icon(int mode, boolean allowPulse) {
        return switch (displayMode(mode, allowPulse)) {
            case 0 -> new ItemStack(Items.GUNPOWDER);
            case 1 -> new ItemStack(Items.REDSTONE);
            case 2 -> ItemStack.EMPTY;
            case 3 -> new ItemStack(Items.REPEATER);
            case 4 -> new ItemStack(Items.BARRIER);
            default -> new ItemStack(Items.REDSTONE);
        };
    }

    @Nullable
    public static ResourceLocation overlay(int mode, boolean allowPulse) {
        return displayMode(mode, allowPulse) == RedstoneMode.HIGH.getId() ? REDSTONE_GUI : null;
    }

    /** Multi-line tooltip: current mode title, description, click hint. */
    public static Component tooltip(int mode, boolean allowPulse) {
        RedstoneMode m = RedstoneMode.fromId(displayMode(mode, allowPulse));
        String key = m.name().toLowerCase();
        return Component.translatable("gui.colossal_reactors.redstone_mode.title." + key)
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.colossal_reactors.redstone_port.mode." + key)
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.colossal_reactors.redstone_mode.hint")
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    public static ItemIconButton button(int x, int y, Button.OnPress onPress, Supplier<Integer> mode, boolean allowPulse) {
        return new ItemIconButton(
                x,
                y,
                ICON_SIZE,
                onPress,
                () -> icon(mode.get(), allowPulse),
                () -> overlay(mode.get(), allowPulse),
                Component.empty());
    }

    public static void refreshTooltip(ItemIconButton button, int mode, boolean allowPulse) {
        if (button != null) {
            button.setTooltip(Tooltip.create(tooltip(mode, allowPulse)));
        }
    }
}
