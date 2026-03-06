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
 * Texture is tiled to fill the area, not stretched.
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
