package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.blockentity.RadiationScrubberBlockEntity;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;

import javax.annotation.Nullable;

/**
 * Client-only gas tank rendering via Mekanism {@link MekanismGasTankRenderer} when the mod is present.
 */
public final class GasTankRenderHelper {

    private GasTankRenderHelper() {}

    /**
     * Draws gas fill using Mek gauge layout ({@code GuiChemicalGauge}).
     *
     * @param outerLeft   screen X of outer tank bounds
     * @param outerTop    screen Y of outer tank top
     * @param outerWidth  outer width in pixels
     * @param outerHeight outer height in pixels
     * @param fillPx      fill height from bottom
     * @return true if Mek rendering succeeded
     */
    public static boolean drawGasInTank(
            GuiGraphics guiGraphics,
            @Nullable String gasRegistryName,
            long amountMb,
            int outerLeft,
            int outerTop,
            int outerWidth,
            int outerHeight,
            int fillPx) {
        if (gasRegistryName == null || gasRegistryName.isEmpty() || amountMb <= 0 || fillPx <= 0) {
            return false;
        }
        if (!MekChemicalHelper.isLoaded()) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(gasRegistryName);
        if (id == null) {
            return false;
        }
        return MekanismGasTankRenderer.draw(guiGraphics, id, amountMb, outerLeft, outerTop, outerWidth, outerHeight, fillPx);
    }

    /** @deprecated Use {@link #drawGasInTank(GuiGraphics, String, long, int, int, int, int, int)} */
    @Deprecated
    public static void drawGasInTank(GuiGraphics guiGraphics, GasRenderInfo info, int x, int y, int width, int height) {
        if (info == null || info.isEmpty() || width <= 0 || height <= 0) {
            return;
        }
        String name = info.registryName();
        if (name != null && MekChemicalHelper.isLoaded()) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id != null && MekanismGasTankRenderer.draw(guiGraphics, id, 1, x, y, width, height, height)) {
                return;
            }
        }
        drawLegacySprite(guiGraphics, info.sprite(), info.tintArgb(), x, y, width, height);
    }

    @Nullable
    public static Component getGasDisplayName(String gasRegistryName) {
        if (gasRegistryName == null || gasRegistryName.isEmpty() || !MekChemicalHelper.isLoaded()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(gasRegistryName);
        if (id == null) return null;
        try {
            var chemical = mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.get(id);
            if (chemical == null) return Component.translatable(gasRegistryName);
            var stack = new mekanism.api.chemical.ChemicalStack(
                    mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical), 1);
            return stack.getTextComponent();
        } catch (Throwable ignored) {
            return Component.translatable(gasRegistryName);
        }
    }

    public static GasRenderInfo getGasRenderInfo(RadiationScrubberBlockEntity blockEntity) {
        if (blockEntity == null) return GasRenderInfo.EMPTY;
        Object handler = blockEntity.getChemicalHandler();
        if (handler == null) return GasRenderInfo.EMPTY;
        try {
            Object stack = handler.getClass().getMethod("getChemicalInTank", int.class).invoke(handler, 0);
            return gasRenderInfoFromStack(stack);
        } catch (Throwable ignored) {
            return GasRenderInfo.EMPTY;
        }
    }

    @Nullable
    public static GasRenderInfo getGasRenderInfoFromRegistryName(String gasRegistryName) {
        if (gasRegistryName == null || gasRegistryName.isEmpty() || !MekChemicalHelper.isLoaded()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(gasRegistryName);
        if (id == null) return null;
        try {
            var chemical = mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.get(id);
            if (chemical == null) return null;
            var stack = new mekanism.api.chemical.ChemicalStack(
                    mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical), 1);
            return gasRenderInfoFromStack(stack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static GasRenderInfo gasRenderInfoFromStack(@Nullable Object stack) {
        if (stack == null || MekChemicalHelper.isEmpty(stack)) {
            return GasRenderInfo.EMPTY;
        }
        String name = MekChemicalHelper.getTypeRegistryName(stack);
        try {
            TextureAtlasSprite sprite = mekanism.client.render.MekanismRenderer.getChemicalTexture(
                    (mekanism.api.chemical.ChemicalStack) stack);
            int tint = ((mekanism.api.chemical.ChemicalStack) stack).getChemicalTint();
            return new GasRenderInfo(name, sprite, tint);
        } catch (Throwable ignored) {
            return new GasRenderInfo(name, null, 0xFFFFFF);
        }
    }

    public record GasRenderInfo(@Nullable String registryName, @Nullable TextureAtlasSprite sprite, int tintArgb) {
        public static final GasRenderInfo EMPTY = new GasRenderInfo(null, null, 0);

        public boolean isEmpty() {
            return registryName == null && sprite == null;
        }

        /** @deprecated use {@link #registryName()} */
        @Deprecated
        @Nullable
        public Object stack() {
            return null;
        }
    }

    private static final int TILE_SIZE = 16;

    private static void drawLegacySprite(
            GuiGraphics guiGraphics, TextureAtlasSprite sprite, int tintArgb, int x, int y, int width, int height) {
        if (sprite == null || width <= 0 || height <= 0) return;
        float r = ((tintArgb >> 16) & 0xFF) / 255f;
        float g = ((tintArgb >> 8) & 0xFF) / 255f;
        float b = (tintArgb & 0xFF) / 255f;
        guiGraphics.setColor(r, g, b, 1f);
        for (int dy = 0; dy < height; dy += TILE_SIZE) {
            for (int dx = 0; dx < width; dx += TILE_SIZE) {
                int w = Math.min(TILE_SIZE, width - dx);
                int h = Math.min(TILE_SIZE, height - dy);
                guiGraphics.blit(x + dx, y + dy, 0, w, h, sprite);
            }
        }
        guiGraphics.setColor(1f, 1f, 1f, 1f);
    }
}
