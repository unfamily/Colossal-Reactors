package net.unfamily.colossal_reactors;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.fluid.ModFluids;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;
import net.unfamily.colossal_reactors.blockentity.PowerPortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.item.ModCreativeModeTabs;
import net.unfamily.colossal_reactors.item.ModItems;
import net.unfamily.colossal_reactors.menu.ModMenuTypes;
import net.unfamily.colossal_reactors.blockentity.HeatingCoilBlockEntity;
import net.unfamily.colossal_reactors.blockentity.RadiationScrubberBlockEntity;
import net.unfamily.colossal_reactors.client.ColossalClientSetup;
import net.unfamily.colossal_reactors.client.ColossalFluidModels;
import net.unfamily.colossal_reactors.datapack.LoadDataReloadListener;
import net.unfamily.colossal_reactors.datapack.ReactorDataReloadListener;
import net.unfamily.colossal_reactors.network.ModPayloads;

@Mod(ColossalReactors.MODID)
public class ColossalReactors {
    public static final String MODID = "colossal_reactors";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier REACTOR_DATA_RELOAD_ID = Identifier.fromNamespaceAndPath(MODID, "reactor_data");
    private static final Identifier LOAD_DATA_RELOAD_ID = Identifier.fromNamespaceAndPath(MODID, "load_data");

    public ColossalReactors(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.addListener(this::onAddServerReloadListeners);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModPayloads.register(modEventBus);
        net.unfamily.colossal_reactors.data.ModConditions.CONDITION_CODECS.register(modEventBus);
        net.unfamily.colossal_reactors.world.ModBiomeModifiers.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modEventBus.addListener(AddClientReloadListenersEvent.class, ColossalReactors::onAddClientReloadListeners);
            modEventBus.addListener(RegisterFluidModelsEvent.class, ColossalFluidModels::registerFluidModels);
            modEventBus.addListener(RegisterMenuScreensEvent.class, ColossalClientSetup::registerMenuScreens);
            modEventBus.addListener(RegisterPayloadHandlersEvent.class, ColossalClientSetup::registerPayloadHandlers);
        }
    }

    /** Server / integrated server: fuel, coolant, melter JSON under data namespaces recipe paths; heating coils under load paths. */
    private void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(REACTOR_DATA_RELOAD_ID, new ReactorDataReloadListener());
        event.addListener(LOAD_DATA_RELOAD_ID, new LoadDataReloadListener());
    }

    /** Client resource reload (JEI, previews): same listeners so datapack merges match single-player expectations. */
    private static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(REACTOR_DATA_RELOAD_ID, new ReactorDataReloadListener());
        event.addListener(LOAD_DATA_RELOAD_ID, new LoadDataReloadListener());
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.RESOURCE_PORT_BE.get(),
                (be, direction) -> ((ResourcePortBlockEntity) be).getItemResourceHandlerForCapability());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.RESOURCE_PORT_BE.get(),
                (be, direction) -> ((ResourcePortBlockEntity) be).getFluidResourceHandlerForCapability());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.REACTOR_BUILDER_BE.get(),
                (be, direction) -> ((ReactorBuilderBlockEntity) be).getFluidResourceCapability());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.POWER_PORT_BE.get(),
                (be, direction) -> ((PowerPortBlockEntity) be).getEnergyHandlerForCapability());
        // Heating coil: by default only front face accepts items/fluids/energy; all_sides overrides that; no_* disables type
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.HEATING_COIL_BE.get(),
                (be, direction) -> ((HeatingCoilBlockEntity) be).allowsCapabilityOnSide(direction)
                        && ((HeatingCoilBlockEntity) be).acceptsItemCapability()
                        ? ((HeatingCoilBlockEntity) be).getItemResourceHandlerForCapability()
                        : null);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.HEATING_COIL_BE.get(),
                (be, direction) -> ((HeatingCoilBlockEntity) be).allowsCapabilityOnSide(direction)
                        && ((HeatingCoilBlockEntity) be).acceptsFluidCapability()
                        ? ((HeatingCoilBlockEntity) be).getFluidResourceHandlerForCapability()
                        : null);
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.HEATING_COIL_BE.get(),
                (be, direction) -> ((HeatingCoilBlockEntity) be).allowsCapabilityOnSide(direction)
                        && ((HeatingCoilBlockEntity) be).acceptsEnergyCapability()
                        ? ((HeatingCoilBlockEntity) be).getEnergyHandlerForCapability()
                        : null);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.MELTER_BE.get(),
                (be, direction) -> ((net.unfamily.colossal_reactors.blockentity.MelterBlockEntity) be).getItemResourceHandlerForCapability());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.MELTER_BE.get(),
                (be, direction) -> ((net.unfamily.colossal_reactors.blockentity.MelterBlockEntity) be).getFluidResourceHandlerForCapability());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.RADIATION_SCRUBBER_BE.get(),
                (be, direction) -> ((net.unfamily.colossal_reactors.blockentity.RadiationScrubberBlockEntity) be).getItemResourceHandlerForCapability());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.RADIATION_SCRUBBER_BE.get(),
                (be, direction) -> ((net.unfamily.colossal_reactors.blockentity.RadiationScrubberBlockEntity) be).getEnergyHandlerForCapability());
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

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.debug("Colossal Reactors common setup");
        LOGGER.info("Reactor validation debug (dev.001_reactor_validation_debug): {}", Config.REACTOR_VALIDATION_DEBUG.get());
        LOGGER.info("Reactor simulation debug (dev.002_reactor_simulation_debug): {}", Config.REACTOR_SIMULATION_DEBUG.get());
    }

    // NOTE: Datagen uses GatherDataEvent.Server / GatherDataEvent.Client on NeoForge 26+.
}
