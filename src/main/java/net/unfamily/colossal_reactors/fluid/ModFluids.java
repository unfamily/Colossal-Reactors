package net.unfamily.colossal_reactors.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.BreeziumBlock;
import net.unfamily.colossal_reactors.block.EnderGooBlock;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.item.ModItems;
import net.unfamily.iskalib.liquid.IskaLibLiquids;
import net.unfamily.iskalib.liquid.LiquidRegistrationRegisters;
import net.unfamily.iskalib.liquid.LiquidSpec;
import net.unfamily.iskalib.liquid.RegisteredLiquid;

/**
 * Fluid deferred registers and registration via Iskandert Library ({@link IskaLibLiquids}).
 */
public final class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES, ColossalReactors.MODID);
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, ColossalReactors.MODID);

    private static final Identifier WATER_OVERLAY =
            Identifier.withDefaultNamespace("block/water_overlay");

    public static RegisteredLiquid MOLTEN_TOUGH_ALLOY;
    public static RegisteredLiquid MOLTEN_STAINLESS_STEEL;
    public static RegisteredLiquid GELID_BREEZIUM;
    public static RegisteredLiquid ENDER_GOO;

    private ModFluids() {}

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);

        var registers = new LiquidRegistrationRegisters(
                FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);

        MOLTEN_TOUGH_ALLOY = IskaLibLiquids.registerLiquid(modEventBus, registers,
                LiquidSpec.withThickLibrarySprites(
                                ColossalReactors.MODID, "molten_tough_alloy", 0xFF5A6A7A,
                                "fluid.colossal_reactors.molten_tough_alloy", 7, true)
                        .withMoltenType()
                        .withBlockLightLevel(7)
                        .withOverlay(WATER_OVERLAY));

        MOLTEN_STAINLESS_STEEL = IskaLibLiquids.registerLiquid(modEventBus, registers,
                LiquidSpec.withThickLibrarySprites(
                                ColossalReactors.MODID, "molten_stainless_steel", 0xFFFF2F23,
                                "fluid.colossal_reactors.molten_stainless_steel", 7, true)
                        .withMoltenType()
                        .withBlockLightLevel(7)
                        .withOverlay(WATER_OVERLAY));

        GELID_BREEZIUM = IskaLibLiquids.registerLiquid(modEventBus, registers,
                LiquidSpec.withThinVanillaWaterSprites(
                                ColossalReactors.MODID, "gelid_breezium", 0xFF00E5FF,
                                "fluid.colossal_reactors.gelid_breezium", 0, true)
                        .withColdWaterType()
                        .withBlockFactory(BreeziumBlock::new));

        ENDER_GOO = IskaLibLiquids.registerLiquid(modEventBus, registers,
                LiquidSpec.withThickLibrarySprites(
                                ColossalReactors.MODID, "ender_goo", 0xFF2c4742,
                                "fluid.colossal_reactors.ender_goo", 7, true)
                        .withMoltenType()
                        .withBlockLightLevel(7)
                        .withBlockFactory(EnderGooBlock::new)
                        .withOverlay(WATER_OVERLAY));
    }
}
