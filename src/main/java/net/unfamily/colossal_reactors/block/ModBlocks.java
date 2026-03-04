package net.unfamily.colossal_reactors.block;

import net.unfamily.colossal_reactors.ColossalReactors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ColossalReactors.MODID);

    public static final DeferredBlock<ReactorGlassBlock> REACTOR_GLASS = BLOCKS.register("reactor_glass",
            ReactorGlassBlock::new);

    public static final DeferredBlock<Block> REACTOR_CASING = BLOCKS.register("reactor_casing",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(2.0f)));

    public static final DeferredBlock<ReactorControllerBlock> REACTOR_CONTROLLER = BLOCKS.register("reactor_controller",
            () -> new ReactorControllerBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(2.0f)
                    .noOcclusion()));

    public static final DeferredBlock<ReactorRodBlock> REACTOR_ROD = BLOCKS.register("reactor_rod",
            () -> new ReactorRodBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(2.0f)
                    .noOcclusion()));

    public static final DeferredBlock<PowerPortBlock> POWER_PORT = BLOCKS.register("power_port",
            () -> new PowerPortBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(2.0f)));
    public static final DeferredBlock<RedstonePortBlock> REDSTONE_PORT = BLOCKS.register("redstone_port",
            () -> new RedstonePortBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(2.0f)));
    public static final DeferredBlock<ResourcePortBlock> RESOURCE_PORT = BLOCKS.register("resource_port",
            () -> new ResourcePortBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(2.0f)));

    public static final DeferredBlock<Block> ROD_CONTROLLER = BLOCKS.register("rod_controller",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(2.0f)));

    public static final DeferredBlock<ReactorBuilderBlock> REACTOR_BUILDER = BLOCKS.register("reactor_builder",
            () -> new ReactorBuilderBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(2.0f)));

    private ModBlocks() {}
}
