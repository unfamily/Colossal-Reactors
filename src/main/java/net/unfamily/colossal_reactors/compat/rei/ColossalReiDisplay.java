package net.unfamily.colossal_reactors.compat.rei;

import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import net.minecraft.resources.Identifier;

public abstract class ColossalReiDisplay extends BasicDisplay {
    private final CategoryIdentifier<ColossalReiDisplay> category;

    protected ColossalReiDisplay(
            CategoryIdentifier<ColossalReiDisplay> category,
            Identifier location,
            List<EntryIngredient> inputs,
            List<EntryIngredient> outputs) {
        super(inputs, outputs, Optional.of(location));
        this.category = category;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    @Override
    public DisplaySerializer<?> getSerializer() {
        return ColossalReiSerializers.EPHEMERAL;
    }

    abstract void drawText(GuiGraphics graphics, int originX, int originY);
}
