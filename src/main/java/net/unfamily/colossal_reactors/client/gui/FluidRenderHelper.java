package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renders fluid in GUI tanks using the fluid's still texture (no tint: tint is for in-world only).
 * Texture is tiled to fill the area, not stretched.
 */
public final class FluidRenderHelper {

    private static final int TILE_SIZE = 16;

    private FluidRenderHelper() {}

    /**
     * Draws fluid in the given rectangle using the fluid's still texture (natural colors, no tint).
     * The texture is tiled to fill the area (cropped on edges if needed), not stretched.
     * Does nothing if stack is empty or fluid has no client extensions.
     *
     * @param guiGraphics GUI graphics
     * @param fluidStack   fluid to draw (amount is ignored; only type and tint matter)
     * @param x            left edge of the fill area
     * @param y            top edge of the fill area (bar fills upward from bottom in typical tank)
     * @param width        width of the fill area
     * @param height       height of the fill area
     */
    public static void drawFluidInTank(GuiGraphics guiGraphics, FluidStack fluidStack, int x, int y, int width, int height) {
        if (fluidStack.isEmpty() || fluidStack.getFluid() == Fluids.EMPTY) {
            return;
        }
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation stillTexture = ext.getStillTexture(fluidStack);
        if (stillTexture == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(stillTexture);
        guiGraphics.setColor(1, 1, 1, 1);

        // Use only the 6-param blit (no UV): same code path as single full rect, so colors stay correct.
        // Each tile draws the full sprite scaled to (w,h); edge tiles are slightly stretched.
        for (int dy = 0; dy < height; dy += TILE_SIZE) {
            for (int dx = 0; dx < width; dx += TILE_SIZE) {
                int w = Math.min(TILE_SIZE, width - dx);
                int h = Math.min(TILE_SIZE, height - dy);
                guiGraphics.blit(x + dx, y + dy, 0, w, h, sprite);
            }
        }

        guiGraphics.setColor(1, 1, 1, 1);
    }
}
