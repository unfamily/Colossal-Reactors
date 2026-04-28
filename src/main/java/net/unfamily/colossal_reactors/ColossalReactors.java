package net.unfamily.colossal_reactors;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.fluid.ModFluids;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;
import net.unfamily.colossal_reactors.blockentity.PowerPortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.item.ModCreativeModeTabs;
import net.unfamily.colossal_reactors.item.ModItems;
import net.unfamily.colossal_reactors.menu.ModMenuTypes;
import net.unfamily.colossal_reactors.datapack.LoadDataReloadListener;
import net.unfamily.colossal_reactors.datapack.ReactorDataReloadListener;
import net.unfamily.colossal_reactors.blockentity.HeatingCoilBlockEntity;
import net.unfamily.colossal_reactors.blockentity.RadiationScrubberBlockEntity;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(ColossalReactors.MODID)
public class ColossalReactors {
    public static final String MODID = "colossal_reactors";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ColossalReactors(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        // networking temporarily disabled for 26.x boot-first milestone
        net.unfamily.colossal_reactors.data.ModConditions.CONDITION_CODECS.register(modEventBus);
        net.unfamily.colossal_reactors.world.ModBiomeModifiers.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RESOURCE_PORT_BE.get(),
                (be, direction) -> ((ResourcePortBlockEntity) be).getItemHandlerForCapability());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.RESOURCE_PORT_BE.get(),
                (be, direction) -> ((ResourcePortBlockEntity) be).getFluidHandlerForCapability());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.REACTOR_BUILDER_BE.get(),
                (be, direction) -> ((ReactorBuilderBlockEntity) be).getFluidTank());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.POWER_PORT_BE.get(),
                (be, direction) -> ((PowerPortBlockEntity) be).getEnergyStorageForCapability());
        // Heating coil: by default only front face accepts items/fluids/energy; all_sides overrides that; no_* disables type
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.HEATING_COIL_BE.get(),
                (be, direction) -> ((HeatingCoilBlockEntity) be).allowsCapabilityOnSide(direction)
                        && ((HeatingCoilBlockEntity) be).acceptsItemCapability()
                        ? ((HeatingCoilBlockEntity) be).getItemHandler()
                        : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.HEATING_COIL_BE.get(),
                (be, direction) -> ((HeatingCoilBlockEntity) be).allowsCapabilityOnSide(direction)
                        && ((HeatingCoilBlockEntity) be).acceptsFluidCapability()
                        ? ((HeatingCoilBlockEntity) be).getFluidHandler()
                        : null);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.HEATING_COIL_BE.get(),
                (be, direction) -> ((HeatingCoilBlockEntity) be).allowsCapabilityOnSide(direction)
                        && ((HeatingCoilBlockEntity) be).acceptsEnergyCapability()
                        ? ((HeatingCoilBlockEntity) be).getEnergyStorage()
                        : null);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MELTER_BE.get(),
                (be, direction) -> ((net.unfamily.colossal_reactors.blockentity.MelterBlockEntity) be).getItemHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MELTER_BE.get(),
                (be, direction) -> ((net.unfamily.colossal_reactors.blockentity.MelterBlockEntity) be).getFluidHandlerForCapability());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RADIATION_SCRUBBER_BE.get(),
                (be, direction) -> ((net.unfamily.colossal_reactors.blockentity.RadiationScrubberBlockEntity) be).getItemHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.RADIATION_SCRUBBER_BE.get(),
                (be, direction) -> ((net.unfamily.colossal_reactors.blockentity.RadiationScrubberBlockEntity) be).getEnergyStorage());
        registerRadiationScrubberChemicalCapability(event);
    }

    /** Registers Mekanism CHEMICAL block capability for Radiation Scrubber when Mekanism is loaded (reflection). */
    @SuppressWarnings("unchecked")
    private static void registerRadiationScrubberChemicalCapability(RegisterCapabilitiesEvent event) {
        try {
            if (!net.neoforged.fml.ModList.get().isLoaded("mekanism")) return;
            Class<?> capsClass = Class.forName("mekanism.common.capabilities.Capabilities");
            Object chemicalMulti = capsClass.getField("CHEMICAL").get(null);
            Object blockCap = chemicalMulti.getClass().getMethod("block").invoke(chemicalMulti);
            event.registerBlockEntity(
                    (BlockCapability<Object, Direction>) blockCap,
                    ModBlockEntities.RADIATION_SCRUBBER_BE.get(),
                    (RadiationScrubberBlockEntity be, Direction direction) -> be.getChemicalHandler());
        } catch (Throwable t) {
            LOGGER.debug("Could not register Radiation Scrubber chemical capability: {}", t.getMessage());
        }
    }

    private void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var packOutput = generator.getPackOutput();
        // Fusion models are currently disabled until Fusion is updated for 26.x.
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.debug("Colossal Reactors common setup");
        LOGGER.info("Reactor validation debug (dev.001_reactor_validation_debug): {}", Config.REACTOR_VALIDATION_DEBUG.get());
        LOGGER.info("Reactor simulation debug (dev.002_reactor_simulation_debug): {}", Config.REACTOR_SIMULATION_DEBUG.get());
    }

    // NOTE: client integration + reload listeners need updating for 26.x and are temporarily disabled
    // to reach a boot-first milestone with minimal dependencies.
}
