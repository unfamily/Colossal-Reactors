package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.registries.Registries;
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
                    () -> BlockEntityType.Builder.of(ReactorControllerBlockEntity::new,
                            ModBlocks.REACTOR_CONTROLLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorRodBlockEntity>> REACTOR_ROD_BE =
            BLOCK_ENTITY_TYPES.register("reactor_rod",
                    () -> BlockEntityType.Builder.of(ReactorRodBlockEntity::new,
                            ModBlocks.REACTOR_ROD.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourcePortBlockEntity>> RESOURCE_PORT_BE =
            BLOCK_ENTITY_TYPES.register("resource_port",
                    () -> BlockEntityType.Builder.of(ResourcePortBlockEntity::new,
                            ModBlocks.RESOURCE_PORT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerPortBlockEntity>> POWER_PORT_BE =
            BLOCK_ENTITY_TYPES.register("power_port",
                    () -> BlockEntityType.Builder.of(PowerPortBlockEntity::new,
                            ModBlocks.POWER_PORT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstonePortBlockEntity>> REDSTONE_PORT_BE =
            BLOCK_ENTITY_TYPES.register("redstone_port",
                    () -> BlockEntityType.Builder.of(RedstonePortBlockEntity::new,
                            ModBlocks.REDSTONE_PORT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorBuilderBlockEntity>> REACTOR_BUILDER_BE =
            BLOCK_ENTITY_TYPES.register("reactor_builder",
                    () -> BlockEntityType.Builder.of(ReactorBuilderBlockEntity::new,
                            ModBlocks.REACTOR_BUILDER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatingCoilBlockEntity>> HEATING_COIL_BE =
            BLOCK_ENTITY_TYPES.register("heating_coil",
                    () -> BlockEntityType.Builder.of(HeatingCoilBlockEntity::new,
                            ModBlocks.HEATING_COIL_BLOCKS.stream().map(b -> b.get()).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MelterBlockEntity>> MELTER_BE =
            BLOCK_ENTITY_TYPES.register("melter",
                    () -> BlockEntityType.Builder.of(MelterBlockEntity::new, ModBlocks.MELTER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    private ModBlockEntities() {}
}
