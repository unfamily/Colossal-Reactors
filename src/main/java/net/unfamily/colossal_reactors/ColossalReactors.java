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
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
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
import net.unfamily.colossal_reactors.data.ColossalReactorsFusionModelProvider;
import net.unfamily.colossal_reactors.item.ModCreativeModeTabs;
import net.unfamily.colossal_reactors.item.ModItems;
import net.unfamily.colossal_reactors.menu.ModMenuTypes;
import net.unfamily.colossal_reactors.network.ModPayloads;
import net.unfamily.colossal_reactors.network.ReactorPreviewMarkerPayload;
import net.unfamily.colossal_reactors.client.gui.ReactorBuilderScreen;
import net.unfamily.colossal_reactors.client.gui.ReactorControllerScreen;
import net.unfamily.colossal_reactors.client.gui.RedstonePortScreen;
import net.unfamily.colossal_reactors.client.gui.ResourcePortScreen;
import net.unfamily.colossal_reactors.datapack.LoadDataReloadListener;
import net.unfamily.colossal_reactors.datapack.ReactorDataReloadListener;
import net.unfamily.colossal_reactors.blockentity.HeatingCoilBlockEntity;
import net.unfamily.colossal_reactors.client.gui.HeatingCoilScreen;
import net.unfamily.colossal_reactors.blockentity.RadiationScrubberBlockEntity;

import net.unfamily.colossal_reactors.client.ClientPayloadHandlers;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
        ModPayloads.register(modEventBus);
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
        generator.addProvider(event.includeClient(), new ColossalReactorsFusionModelProvider(packOutput));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.debug("Colossal Reactors common setup");
        LOGGER.info("Reactor validation debug (dev.001_reactor_validation_debug): {}", Config.REACTOR_VALIDATION_DEBUG.get());
        LOGGER.info("Reactor simulation debug (dev.002_reactor_simulation_debug): {}", Config.REACTOR_SIMULATION_DEBUG.get());
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReactorDataReloadListener());
        event.addListener(new LoadDataReloadListener());
    }

    @EventBusSubscriber(modid = ColossalReactors.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.REACTOR_GLASS.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.REACTOR_ROD.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModFluids.MOLTEN_TOUGH_ALLOY.block().get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(ModFluids.GELID_BREEZIUM.block().get(), RenderType.translucent());
            });
        }

        @SubscribeEvent
        static void registerClientPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar(ColossalReactors.MODID).versioned("1")
                    .playToClient(ReactorPreviewMarkerPayload.TYPE, ReactorPreviewMarkerPayload.STREAM_CODEC, ClientPayloadHandlers::handlePreviewMarker);
        }

        @SubscribeEvent
        static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new ReactorDataReloadListener());
        }

        @SubscribeEvent
        static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.RESOURCE_PORT_MENU.get(), ResourcePortScreen::new);
            event.register(ModMenuTypes.REDSTONE_PORT_MENU.get(), RedstonePortScreen::new);
            event.register(ModMenuTypes.REACTOR_CONTROLLER_MENU.get(), ReactorControllerScreen::new);
            event.register(ModMenuTypes.REACTOR_BUILDER_MENU.get(), ReactorBuilderScreen::new);
            event.register(ModMenuTypes.HEATING_COIL_MENU.get(), HeatingCoilScreen::new);
            event.register(ModMenuTypes.MELTER_MENU.get(), net.unfamily.colossal_reactors.client.gui.MelterScreen::new);
            event.register(ModMenuTypes.RADIATION_SCRUBBER_MENU.get(), net.unfamily.colossal_reactors.client.gui.RadiationScrubberScreen::new);
        }
    }
}
