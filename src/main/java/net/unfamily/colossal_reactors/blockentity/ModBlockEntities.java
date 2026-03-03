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

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    private ModBlockEntities() {}
}
