package net.unfamily.colossal_reactors.compat.rei;

import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public abstract class ColossalReiDisplay extends BasicDisplay {
    private final CategoryIdentifier<ColossalReiDisplay> category;

    protected ColossalReiDisplay(
            CategoryIdentifier<ColossalReiDisplay> category,
            ResourceLocation location,
            List<EntryIngredient> inputs,
            List<EntryIngredient> outputs) {
        super(inputs, outputs, Optional.of(location));
        this.category = category;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    abstract void drawText(GuiGraphics graphics, int originX, int originY);
}
