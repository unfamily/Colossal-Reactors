package net.unfamily.colossal_reactors.compat;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;

/**
 * Shared category / recipe UIDs for JEI, EMI and REI.
 * Must not reference any recipe-viewer API class (viewers are mutually exclusive at runtime).
 */
public final class RecipeViewerIds {
    public static final ResourceLocation REACTOR_COOLANT = id("reactor_coolant");
    public static final ResourceLocation REACTOR_FUEL = id("reactor_fuel");
    public static final ResourceLocation REACTOR_HEAT_SINK = id("reactor_heat_sink");
    public static final ResourceLocation MELTER = id("melter");
    public static final ResourceLocation MELTER_HEAT_SOURCE = id("melter_heat_source");
    public static final ResourceLocation HEATING_COIL = id("heating_coil");
    public static final ResourceLocation ELEC_COIL = id("elec_coil");
    public static final ResourceLocation TURBINE_GENERATION = id("turbine_generation");

    private RecipeViewerIds() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, path);
    }
}
