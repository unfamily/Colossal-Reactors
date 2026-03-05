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

    // Resource ores (stone + deepslate)
    public static final DeferredBlock<Block> URANIUM_ORE = BLOCKS.register("uranium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE = BLOCKS.register("deep_uranium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.DEEPSLATE)
                    .strength(4.5f, 3.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> LEAD_ORE = BLOCKS.register("lead_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_LEAD_ORE = BLOCKS.register("deep_lead_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.DEEPSLATE)
                    .strength(4.5f, 3.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BORON_ORE = BLOCKS.register("boron_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_BORON_ORE = BLOCKS.register("deep_boron_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.DEEPSLATE)
                    .strength(4.5f, 3.0f)
                    .requiresCorrectToolForDrops()));

    // Storage and raw blocks
    public static final DeferredBlock<Block> URANIUM_BLOCK = BLOCKS.register("uranium_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> URANIUM_RAW_BLOCK = BLOCKS.register("uranium_raw_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> LEAD_BLOCK = BLOCKS.register("lead_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> RAW_LEAD_BLOCK = BLOCKS.register("raw_lead_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BORON_BLOCK = BLOCKS.register("boron_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> RAW_BORON_BLOCK = BLOCKS.register("raw_boron_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> GRAPHITE_BLOCK = BLOCKS.register("graphite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> AZURITE_BLOCK = BLOCKS.register("azurite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> TOUGH_ALLOY_BLOCK = BLOCKS.register("tough_alloy_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()));

    private ModBlocks() {}
}
