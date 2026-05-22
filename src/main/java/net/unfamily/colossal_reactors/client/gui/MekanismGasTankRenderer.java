package net.unfamily.colossal_reactors.client.gui;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.GuiUtils.TilingDirection;
import mekanism.client.render.MekanismRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Direct Mekanism chemical tank draw ({@link mekanism.client.gui.element.gauge.GuiChemicalGauge}).
 * Only call when Mekanism is loaded ({@link net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper#isLoaded()}).
 */
public final class MekanismGasTankRenderer {

    private static final int TEXTURE_TILE = 16;
    /** No inset: GUI texture coords are the full inner fill area (frame is outside these bounds). */
    public static final int GAUGE_INSET = 0;
    private static final int Z_LEVEL = 100;

    private MekanismGasTankRenderer() {}

    /**
     * @param outerLeft   screen X of tank outer bounds (e.g. {@link ResourcePortGuiLayout#GAS_BAR_X} + guiX)
     * @param outerTop    screen Y of tank outer top
     * @param outerWidth  outer width (e.g. 12)
     * @param outerHeight outer height (e.g. 54)
     * @param fillPx      filled height in pixels from bottom
     */
    public static boolean draw(
            GuiGraphics guiGraphics,
            @Nullable ResourceLocation chemicalId,
            long amountMb,
            int outerLeft,
            int outerTop,
            int outerWidth,
            int outerHeight,
            int fillPx) {
        if (chemicalId == null || amountMb <= 0 || fillPx <= 0) {
            return false;
        }
        ChemicalStack stack = resolveStack(chemicalId, amountMb);
        if (stack.isEmpty()) {
            return false;
        }
        TextureAtlasSprite sprite = MekanismRenderer.getChemicalTexture(stack);
        if (sprite == null) {
            return false;
        }

        int innerW = outerWidth - 2 * GAUGE_INSET;
        int innerH = outerHeight - 2 * GAUGE_INSET;
        if (innerW <= 0 || innerH <= 0) {
            return false;
        }

        int scale = Math.min(fillPx, innerH);
        if (amountMb > 0 && scale < 1) {
            scale = 1;
        }
        if (scale <= 0) {
            return false;
        }

        int x = outerLeft + GAUGE_INSET;
        int y = outerTop + GAUGE_INSET;

        MekanismRenderer.color(guiGraphics, stack);
        GuiUtils.drawTiledSprite(
                guiGraphics,
                x,
                y,
                innerH,
                innerW,
                scale,
                sprite,
                TEXTURE_TILE,
                TEXTURE_TILE,
                Z_LEVEL,
                TilingDirection.UP_RIGHT,
                true
        );
        MekanismRenderer.resetColor(guiGraphics);
        return true;
    }

    private static ChemicalStack resolveStack(ResourceLocation id, long amount) {
        Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.get(id);
        if (chemical == null) {
            return ChemicalStack.EMPTY;
        }
        return new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical), amount);
    }
}
