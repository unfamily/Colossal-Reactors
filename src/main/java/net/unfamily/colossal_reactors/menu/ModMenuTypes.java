package net.unfamily.colossal_reactors.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.colossal_reactors.ColossalReactors;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ColossalReactors.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ResourcePortMenu>> RESOURCE_PORT_MENU =
            MENUS.register("resource_port", () ->
                    new MenuType<>(ResourcePortMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<RedstonePortMenu>> REDSTONE_PORT_MENU =
            MENUS.register("redstone_port", () ->
                    new MenuType<>(RedstonePortMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorControllerMenu>> REACTOR_CONTROLLER_MENU =
            MENUS.register("reactor_controller", () ->
                    new MenuType<>(ReactorControllerMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorBuilderMenu>> REACTOR_BUILDER_MENU =
            MENUS.register("reactor_builder", () ->
                    new MenuType<>(ReactorBuilderMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<HeatingCoilMenu>> HEATING_COIL_MENU =
            MENUS.register("heating_coil", () ->
                    new MenuType<>(HeatingCoilMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<MelterMenu>> MELTER_MENU =
            MENUS.register("melter", () ->
                    new MenuType<>(MelterMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<RadiationScrubberMenu>> RADIATION_SCRUBBER_MENU =
            MENUS.register("radiation_scrubber", () ->
                    new MenuType<>(RadiationScrubberMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
