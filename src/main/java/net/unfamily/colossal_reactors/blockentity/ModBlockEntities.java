package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ColossalReactors.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorControllerBlockEntity>> REACTOR_CONTROLLER_BE =
            BLOCK_ENTITY_TYPES.register("reactor_controller",
                    () -> new BlockEntityType<>(ReactorControllerBlockEntity::new, ModBlocks.REACTOR_CONTROLLER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorRodBlockEntity>> REACTOR_ROD_BE =
            BLOCK_ENTITY_TYPES.register("reactor_rod",
                    () -> new BlockEntityType<>(ReactorRodBlockEntity::new, ModBlocks.REACTOR_ROD.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourcePortBlockEntity>> RESOURCE_PORT_BE =
            BLOCK_ENTITY_TYPES.register("resource_port",
                    () -> new BlockEntityType<>(ResourcePortBlockEntity::new, ModBlocks.RESOURCE_PORT.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerPortBlockEntity>> POWER_PORT_BE =
            BLOCK_ENTITY_TYPES.register("power_port",
                    () -> new BlockEntityType<>(PowerPortBlockEntity::new, ModBlocks.POWER_PORT.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstonePortBlockEntity>> REDSTONE_PORT_BE =
            BLOCK_ENTITY_TYPES.register("redstone_port",
                    () -> new BlockEntityType<>(RedstonePortBlockEntity::new, ModBlocks.REDSTONE_PORT.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorBuilderBlockEntity>> REACTOR_BUILDER_BE =
            BLOCK_ENTITY_TYPES.register("reactor_builder",
                    () -> new BlockEntityType<>(ReactorBuilderBlockEntity::new, ModBlocks.REACTOR_BUILDER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatingCoilBlockEntity>> HEATING_COIL_BE =
            BLOCK_ENTITY_TYPES.register("heating_coil",
                    () -> new BlockEntityType<>(HeatingCoilBlockEntity::new,
                            ModBlocks.HEATING_COIL_BLOCKS.stream().map(net.neoforged.neoforge.registries.DeferredBlock::get).toArray(Block[]::new)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MelterBlockEntity>> MELTER_BE =
            BLOCK_ENTITY_TYPES.register("melter",
                    () -> new BlockEntityType<>(MelterBlockEntity::new, ModBlocks.MELTER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadiationScrubberBlockEntity>> RADIATION_SCRUBBER_BE =
            BLOCK_ENTITY_TYPES.register("radiation_scrubber",
                    () -> new BlockEntityType<>(RadiationScrubberBlockEntity::new, ModBlocks.RADIATION_SCRUBBER.get()));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    private ModBlockEntities() {}
}
