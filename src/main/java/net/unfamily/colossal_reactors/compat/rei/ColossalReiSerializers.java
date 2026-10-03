package net.unfamily.colossal_reactors.compat.rei;

import com.mojang.serialization.MapCodec;
import java.util.List;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;

public final class ColossalReiSerializers {
    private static final ColossalReiDisplay PLACEHOLDER = new ColossalReiDisplay(
            ColossalReiCategories.FUEL,
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "rei_placeholder"),
            List.of(EntryIngredient.empty()),
            List.of()) {
        @Override
        void drawText(GuiGraphics graphics, int originX, int originY) {}
    };

    public static final DisplaySerializer<ColossalReiDisplay> EPHEMERAL = DisplaySerializer.of(
            MapCodec.unit(PLACEHOLDER),
            StreamCodec.unit(PLACEHOLDER),
            false);

    private ColossalReiSerializers() {}
}
