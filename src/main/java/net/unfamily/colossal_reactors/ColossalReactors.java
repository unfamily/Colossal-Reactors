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
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.PowerPortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;
import net.unfamily.colossal_reactors.data.ColossalReactorsFusionModelProvider;
import net.unfamily.colossal_reactors.item.ModCreativeModeTabs;
import net.unfamily.colossal_reactors.item.ModItems;
import net.unfamily.colossal_reactors.menu.ModMenuTypes;
import net.unfamily.colossal_reactors.network.ModPayloads;
import net.unfamily.colossal_reactors.client.gui.ReactorControllerScreen;
import net.unfamily.colossal_reactors.client.gui.RedstonePortScreen;
import net.unfamily.colossal_reactors.client.gui.ResourcePortScreen;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.docs.ScriptsDocsGenerator;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

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
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModPayloads.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RESOURCE_PORT_BE.get(),
                (be, direction) -> ((ResourcePortBlockEntity) be).getItemHandlerForCapability());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.RESOURCE_PORT_BE.get(),
                (be, direction) -> ((ResourcePortBlockEntity) be).getFluidHandlerForCapability());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.POWER_PORT_BE.get(),
                (be, direction) -> ((PowerPortBlockEntity) be).getEnergyStorageForCapability());
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
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.debug("Colossal Reactors server starting");
        // Always overwrite README in scripts directory so docs stay up to date when we change them
        String basePath = Config.EXTERNAL_SCRIPTS_PATH.get();
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "kubejs/external_scripts/colossal_reactors";
        }
        Path base = Paths.get(basePath);
        Path reactorDir = base.resolve("reactor");
        try {
            ScriptsDocsGenerator.generateReadme(base);
        } catch (Exception e) {
            LOGGER.warn("Could not write scripts README: {}", e.getMessage());
        }
        try {
            FuelLoader.dumpDefaultFile(reactorDir);
            CoolantLoader.dumpDefaultFile(reactorDir);
            HeatSinkLoader.dumpDefaultFile(reactorDir);
        } catch (Exception e) {
            LOGGER.warn("Could not write default script files: {}", e.getMessage());
        }
        FuelLoader.scanConfigDirectory();
        CoolantLoader.scanConfigDirectory();
        HeatSinkLoader.scanConfigDirectory();
    }

    @EventBusSubscriber(modid = ColossalReactors.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.REACTOR_GLASS.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.REACTOR_ROD.get(), RenderType.cutout());
            });
        }

        @SubscribeEvent
        static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.RESOURCE_PORT_MENU.get(), ResourcePortScreen::new);
            event.register(ModMenuTypes.REDSTONE_PORT_MENU.get(), RedstonePortScreen::new);
            event.register(ModMenuTypes.REACTOR_CONTROLLER_MENU.get(), ReactorControllerScreen::new);
        }
    }
}
