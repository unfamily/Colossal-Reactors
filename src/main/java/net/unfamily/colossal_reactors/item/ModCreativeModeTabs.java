package net.unfamily.colossal_reactors.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.ModList;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.fluid.ModFluids;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ColossalReactors.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COLOSSAL_REACTORS_TAB =
            CREATIVE_MODE_TABS.register("colossal_reactors_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.colossal_reactors"))
                            .icon(() -> new ItemStack(ModItems.REACTOR_CONTROLLER.get()))
                            .displayItems((params, output) -> {
                                output.accept(ModItems.REACTOR_CONTROLLER.get());
                                output.accept(ModItems.REACTOR_BUILDER.get());
                                output.accept(ModItems.REACTOR_GLASS.get());
                                output.accept(ModItems.REACTOR_CASING.get());
                                output.accept(ModItems.REACTOR_ROD.get());
                                output.accept(ModItems.ROD_CONTROLLER.get());
                                output.accept(ModItems.POWER_PORT.get());
                                output.accept(ModItems.REDSTONE_PORT.get());
                                output.accept(ModItems.RESOURCE_PORT.get());
                                output.accept(ModItems.MELTER.get());
                                if (ModList.get().isLoaded("mekanism")) output.accept(ModItems.RADIATION_SCRUBBER.get());
                                for (var item : ModItems.HEATING_COIL_OFF_ITEMS) output.accept(item.get());
                                output.accept(ModItems.URANIUM_ORE.get());
                                output.accept(ModItems.DEEPSLATE_URANIUM_ORE.get());
                                output.accept(ModItems.LEAD_ORE.get());
                                output.accept(ModItems.DEEPSLATE_LEAD_ORE.get());
                                output.accept(ModItems.BORON_ORE.get());
                                output.accept(ModItems.DEEPSLATE_BORON_ORE.get());
                                output.accept(ModItems.URANIUM_BLOCK.get());
                                output.accept(ModItems.URANIUM_RAW_BLOCK.get());
                                output.accept(ModItems.LEAD_BLOCK.get());
                                output.accept(ModItems.RAW_LEAD_BLOCK.get());
                                output.accept(ModItems.BORON_BLOCK.get());
                                output.accept(ModItems.RAW_BORON_BLOCK.get());
                                output.accept(ModItems.GRAPHITE_BLOCK.get());
                                output.accept(ModItems.AZURITE_BLOCK.get());
                                output.accept(ModItems.TOUGH_ALLOY_BLOCK.get());
                                output.accept(ModItems.RAW_URANIUM.get());
                                output.accept(ModItems.URANIUM_INGOT.get());
                                output.accept(ModItems.LEAD_RAW.get());
                                output.accept(ModItems.BORON_RAW.get());
                                output.accept(ModItems.BORON_INGOT.get());
                                output.accept(ModItems.GRAPHITE_INGOT.get());
                                output.accept(ModItems.AZURITE_INGOT.get());
                                output.accept(ModItems.LEAD_INGOT.get());
                                output.accept(ModItems.UNREFINED_TOUGH_ALLOY.get());
                                output.accept(ModItems.TOUGH_ALLOY_INGOT.get());
                                output.accept(ModItems.URANIUM_DUST.get());
                                output.accept(ModItems.LEAD_DUST.get());
                                output.accept(ModItems.BORON_DUST.get());
                                output.accept(ModItems.NUCLEAR_WASTE.get());
                                output.accept(ModItems.CATALYST_BREEZIUM.get());
                                output.accept(ModFluids.MOLTEN_TOUGH_ALLOY.bucket().get());
                                output.accept(ModFluids.ENDER_GOO.bucket().get());
                                output.accept(ModFluids.GELID_BREEZIUM.bucket().get());
                            })
                            .build());

    private ModCreativeModeTabs() {}
}
