package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.unfamily.colossal_reactors.blockentity.RadiationScrubberBlockEntity;

import javax.annotation.Nullable;

/**
 * Client-only helper to render Mekanism gas in GUI using the chemical's texture and tint.
 * Uses reflection so no compile-time dependency on Mekanism.
 */
public final class GasTankRenderHelper {

    private static final int TILE_SIZE = 16;

    private GasTankRenderHelper() {}

    /**
     * Result of resolving gas from the scrubber tank: sprite and tint (ARGB) for rendering.
     * Null sprite means use fallback (e.g. solid color).
     */
    public record GasRenderInfo(TextureAtlasSprite sprite, int tintArgb) {
        public boolean isEmpty() {
            return sprite == null;
        }
    }

    /**
     * Gets render info from the synced gas type registry name (from menu data).
     * Use this for GUI rendering so the client shows the correct gas even though the client BE tank is not synced.
     */
    @Nullable
    public static GasRenderInfo getGasRenderInfoFromRegistryName(String gasRegistryName) {
        if (gasRegistryName == null || gasRegistryName.isEmpty()) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        try {
            RegistryAccess regAccess = mc.level.registryAccess();
            Class<?> apiClass = Class.forName("mekanism.api.MekanismAPI");
            Object key = apiClass.getField("CHEMICAL_REGISTRY_NAME").get(null);
            Object registry = regAccess.getClass().getMethod("registryOrThrow", net.minecraft.resources.ResourceKey.class).invoke(regAccess, key);
            ResourceLocation loc = ResourceLocation.parse(gasRegistryName);
            Object chemical = registry.getClass().getMethod("get", ResourceLocation.class).invoke(registry, loc);
            if (chemical == null) return null;
            ResourceLocation icon = (ResourceLocation) chemical.getClass().getMethod("getIcon").invoke(chemical);
            if (icon == null) return null;
            Number tintNum = (Number) chemical.getClass().getMethod("getTint").invoke(chemical);
            int tint = tintNum != null ? tintNum.intValue() : 0xFFFFFF;
            TextureAtlasSprite sprite = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(icon);
            return new GasRenderInfo(sprite, tint);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Gets the display name component for the gas (for tooltip). Uses chemical's translation key.
     */
    @Nullable
    public static Component getGasDisplayName(String gasRegistryName) {
        if (gasRegistryName == null || gasRegistryName.isEmpty()) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        try {
            RegistryAccess regAccess = mc.level.registryAccess();
            Class<?> apiClass = Class.forName("mekanism.api.MekanismAPI");
            Object key = apiClass.getField("CHEMICAL_REGISTRY_NAME").get(null);
            Object registry = regAccess.getClass().getMethod("registryOrThrow", net.minecraft.resources.ResourceKey.class).invoke(regAccess, key);
            ResourceLocation loc = ResourceLocation.parse(gasRegistryName);
            Object chemical = registry.getClass().getMethod("get", ResourceLocation.class).invoke(registry, loc);
            if (chemical == null) return null;
            String translationKey = (String) chemical.getClass().getMethod("getTranslationKey").invoke(chemical);
            return translationKey != null ? Component.translatable(translationKey) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Gets the gas in the scrubber's chemical tank (server-side or when client has content).
     * Prefer {@link #getGasRenderInfoFromRegistryName(String)} for GUI so client shows synced gas.
     */
    public static GasRenderInfo getGasRenderInfo(RadiationScrubberBlockEntity blockEntity) {
        if (blockEntity == null) return new GasRenderInfo(null, 0);
        Object handler = blockEntity.getChemicalHandler();
        if (handler == null) return new GasRenderInfo(null, 0);
        try {
            Object stack = handler.getClass().getMethod("getChemicalInTank", int.class).invoke(handler, 0);
            if (stack == null) return new GasRenderInfo(null, 0);
            Boolean empty = (Boolean) stack.getClass().getMethod("isEmpty").invoke(stack);
            if (Boolean.TRUE.equals(empty)) return new GasRenderInfo(null, 0);

            Object chemical = stack.getClass().getMethod("getChemical").invoke(stack);
            if (chemical == null) return new GasRenderInfo(null, 0);
            ResourceLocation icon = (ResourceLocation) chemical.getClass().getMethod("getIcon").invoke(chemical);
            if (icon == null) return new GasRenderInfo(null, 0);

            Number tintNum = (Number) stack.getClass().getMethod("getChemicalTint").invoke(stack);
            int tint = tintNum != null ? tintNum.intValue() : 0xFFFFFF;

            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(icon);
            return new GasRenderInfo(sprite, tint);
        } catch (Throwable ignored) {
            return new GasRenderInfo(null, 0);
        }
    }

    /**
     * Draws gas in the given rectangle using the chemical's texture (tiled) and tint.
     * If info has no sprite, draws nothing (caller can draw fallback).
     */
    public static void drawGasInTank(net.minecraft.client.gui.GuiGraphics guiGraphics, GasRenderInfo info, int x, int y, int width, int height) {
        if (info == null || info.isEmpty() || width <= 0 || height <= 0) return;

        float r = ((info.tintArgb >> 16) & 0xFF) / 255f;
        float g = ((info.tintArgb >> 8) & 0xFF) / 255f;
        float b = (info.tintArgb & 0xFF) / 255f;
        guiGraphics.setColor(r, g, b, 1f);

        for (int dy = 0; dy < height; dy += TILE_SIZE) {
            for (int dx = 0; dx < width; dx += TILE_SIZE) {
                int w = Math.min(TILE_SIZE, width - dx);
                int h = Math.min(TILE_SIZE, height - dy);
                guiGraphics.blit(x + dx, y + dy, 0, w, h, info.sprite);
            }
        }

        guiGraphics.setColor(1, 1, 1, 1);
    }
}
