package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.compat.jei.CoolantRecipeCategory;
import net.unfamily.colossal_reactors.compat.jei.ElecCoilRecipeCategory;
import net.unfamily.colossal_reactors.compat.jei.FuelRecipeCategory;
import net.unfamily.colossal_reactors.compat.jei.HeatSinkRecipeCategory;
import net.unfamily.colossal_reactors.compat.jei.HeatingCoilRecipeCategory;
import net.unfamily.colossal_reactors.compat.jei.MelterHeatSourceRecipeCategory;
import net.unfamily.colossal_reactors.compat.jei.MelterRecipeCategory;
import net.unfamily.colossal_reactors.compat.jei.TurbineGenerationRecipeCategory;

/** EMI categories aligned with {@link net.unfamily.colossal_reactors.compat.jei} UIDs. */
public final class EmiCategories {
    public static final EmiRecipeCategory COOLANT = category(CoolantRecipeCategory.UID, ModBlocks.RESOURCE_PORT.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory FUEL = category(FuelRecipeCategory.UID, ModBlocks.REACTOR_ROD.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory HEAT_SINK = category(HeatSinkRecipeCategory.UID, ModBlocks.REACTOR_GLASS.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory MELTER = category(MelterRecipeCategory.UID, ModBlocks.MELTER.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory MELTER_HEAT = category(MelterHeatSourceRecipeCategory.UID, ModBlocks.MELTER.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory HEATING_COIL = category(HeatingCoilRecipeCategory.UID, heatingCoilIcon());
    public static final EmiRecipeCategory ELEC_COIL = category(ElecCoilRecipeCategory.UID, ModBlocks.TURBINE_CASING.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory TURBINE_GENERATION = category(TurbineGenerationRecipeCategory.UID, ModBlocks.TURBINE_CONTROLLER.get().asItem().getDefaultInstance());

    public static final EmiRecipeCategory[] ALL = {
            COOLANT, FUEL, HEAT_SINK, MELTER, MELTER_HEAT, HEATING_COIL, ELEC_COIL, TURBINE_GENERATION
    };

    private EmiCategories() {}

    private static EmiRecipeCategory category(Identifier id, ItemStack icon) {
        return new EmiRecipeCategory(id, EmiStack.of(icon));
    }

    private static ItemStack heatingCoilIcon() {
        if (ModBlocks.HEATING_COIL_BLOCKS.isEmpty()) {
            return ModBlocks.MELTER.get().asItem().getDefaultInstance();
        }
        return ModBlocks.HEATING_COIL_BLOCKS.get(0).get().asItem().getDefaultInstance();
    }
}
