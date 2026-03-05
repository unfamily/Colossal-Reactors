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
 * Renders fluid in GUI tanks using the fluid's still texture.
 * Only minecraft:water gets an azure tint in GUI; other fluids use natural colors (no tint).
 * Texture is tiled to fill the area, not stretched.
 */
public final class FluidRenderHelper {

    private static final int TILE_SIZE = 16;
    /** Azure tint for minecraft:water in GUI (RGB 0x3F76E4 normalized). */
    private static final float WATER_TINT_R = 0.247f;
    private static final float WATER_TINT_G = 0.463f;
    private static final float WATER_TINT_B = 0.894f;

    private FluidRenderHelper() {}

    /**
     * Draws fluid in the given rectangle. Only minecraft:water is tinted azure; other fluids unchanged.
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

        if (fluid == Fluids.WATER) {
            guiGraphics.setColor(WATER_TINT_R, WATER_TINT_G, WATER_TINT_B, 1f);
        } else {
            guiGraphics.setColor(1, 1, 1, 1);
        }

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
