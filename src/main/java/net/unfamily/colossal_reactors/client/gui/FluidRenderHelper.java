package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renders fluid in GUI tanks using the baked fluid model still sprite and fluid tint source.
 * Texture is tiled to fill the area, not stretched.
 */
public final class FluidRenderHelper {

    private static final int TILE_SIZE = 16;

    private FluidRenderHelper() {}

    /**
     * Draws fluid in the given rectangle using the fluid world model tint (when defined).
     */
    public static void drawFluidInTank(GuiGraphicsExtractor guiGraphics, FluidStack fluidStack, int x, int y, int width, int height) {
        if (fluidStack.isEmpty() || fluidStack.getFluid() == Fluids.EMPTY) {
            return;
        }
        Fluid fluid = fluidStack.getFluid();
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        FluidTintSource tintSource = model.fluidTintSource();
        int tint = tintSource != null ? tintSource.color(fluid.defaultFluidState()) : 0xFFFFFF;
        int color = ARGB.color(255, (tint >> 16) & 0xFF, (tint >> 8) & 0xFF, tint & 0xFF);

        for (int dy = 0; dy < height; dy += TILE_SIZE) {
            for (int dx = 0; dx < width; dx += TILE_SIZE) {
                int w = Math.min(TILE_SIZE, width - dx);
                int h = Math.min(TILE_SIZE, height - dy);
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + dx, y + dy, w, h, color);
            }
        }
    }
}
