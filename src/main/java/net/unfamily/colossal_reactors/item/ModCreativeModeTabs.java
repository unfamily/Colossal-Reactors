package net.unfamily.colossal_reactors.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.colossal_reactors.ColossalReactors;

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
                                output.accept(ModItems.REACTOR_GLASS.get());
                                output.accept(ModItems.REACTOR_CASING.get());
                                output.accept(ModItems.REACTOR_ROD.get());
                                output.accept(ModItems.POWER_PORT.get());
                                output.accept(ModItems.REDSTONE_PORT.get());
                                output.accept(ModItems.RESOURCE_PORT.get());
                            })
                            .build());

    private ModCreativeModeTabs() {}
}
