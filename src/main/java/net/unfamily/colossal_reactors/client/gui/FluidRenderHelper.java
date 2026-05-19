package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renders fluid in GUI tanks using the fluid's still texture and its tint from IClientFluidTypeExtensions.
 * Texture is tiled from the bottom; partial tiles are clipped (not stretched).
 */
public final class FluidRenderHelper {

    private static final int TILE_SIZE = 16;

    private FluidRenderHelper() {}

    /**
     * Draws fluid in the given rectangle using the fluid's tint (getTintColor() from client extensions).
     * Does nothing if stack is empty or fluid has no client extensions.
     */
    public static void drawFluidInTank(GuiGraphics guiGraphics, FluidStack fluidStack, int x, int y, int width, int height) {
        if (fluidStack.isEmpty() || fluidStack.getFluid() == Fluids.EMPTY) {
            return;
        }
        Fluid fluid = fluidStack.getFluid();
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = ext.getStillTexture(fluidStack);
        if (stillTexture == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(stillTexture);

        int tint = ext.getTintColor();
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        guiGraphics.setColor(r, g, b, 1f);
        drawTiledSpriteBottomUp(guiGraphics, sprite, x, y, width, height);
        guiGraphics.setColor(1, 1, 1, 1);
    }

    private static void drawTiledSpriteBottomUp(GuiGraphics guiGraphics, TextureAtlasSprite sprite, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int remainingHeight = height;
        int tileBottom = y + height;
        while (remainingHeight > 0) {
            int tileH = Math.min(TILE_SIZE, remainingHeight);
            tileBottom -= tileH;
            for (int dx = 0; dx < width; dx += TILE_SIZE) {
                int tileW = Math.min(TILE_SIZE, width - dx);
                int tileX = x + dx;
                guiGraphics.enableScissor(tileX, tileBottom, tileX + tileW, tileBottom + tileH);
                guiGraphics.blit(tileX, tileBottom, 0, TILE_SIZE, TILE_SIZE, sprite);
                guiGraphics.disableScissor();
            }
            remainingHeight -= tileH;
        }
    }
}
