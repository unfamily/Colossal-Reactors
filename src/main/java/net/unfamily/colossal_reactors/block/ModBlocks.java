package net.unfamily.colossal_reactors.block;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Block registrations use {@link DeferredRegister.Blocks#registerBlock} so {@link BlockBehaviour.Properties}
 * receive {@link BlockBehaviour.Properties#setId} before constructors run (required on Minecraft 26+).
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ColossalReactors.MODID);

    /** Heating coil blocks (off and on per coil id); filled in static block below. */
    public static final List<DeferredBlock<HeatingCoilBlock>> HEATING_COIL_BLOCKS = new ArrayList<>();

    public static final DeferredBlock<ReactorGlassBlock> REACTOR_GLASS = BLOCKS.registerBlock("reactor_glass",
            ReactorGlassBlock::new,
            p -> p.sound(SoundType.GLASS)
                    .instrument(NoteBlockInstrument.HAT)
                    .strength(0.3f)
                    .noOcclusion()
                    .isValidSpawn((s, l, pos, e) -> false)
                    .isRedstoneConductor((s, l, pos) -> false)
                    .isSuffocating((s, l, pos) -> false)
                    .isViewBlocking((s, l, pos) -> false));

    public static final DeferredBlock<Block> REACTOR_CASING = BLOCKS.registerBlock("reactor_casing",
            Block::new,
            p -> p.sound(SoundType.METAL).strength(2.0f));

    public static final DeferredBlock<ReactorControllerBlock> REACTOR_CONTROLLER = BLOCKS.registerBlock("reactor_controller",
            ReactorControllerBlock::new,
            p -> p.sound(SoundType.METAL).strength(2.0f).noOcclusion());

    public static final DeferredBlock<ReactorRodBlock> REACTOR_ROD = BLOCKS.registerBlock("reactor_rod",
            ReactorRodBlock::new,
            p -> p.sound(SoundType.METAL).strength(2.0f).noOcclusion());

    public static final DeferredBlock<PowerPortBlock> POWER_PORT = BLOCKS.registerBlock("power_port",
            PowerPortBlock::new,
            p -> p.sound(SoundType.METAL).strength(2.0f));

    public static final DeferredBlock<RedstonePortBlock> REDSTONE_PORT = BLOCKS.registerBlock("redstone_port",
            RedstonePortBlock::new,
            p -> p.sound(SoundType.METAL).strength(2.0f));

    public static final DeferredBlock<ResourcePortBlock> RESOURCE_PORT = BLOCKS.registerBlock("resource_port",
            ResourcePortBlock::new,
            p -> p.sound(SoundType.METAL).strength(2.0f));

    public static final DeferredBlock<MelterBlock> MELTER = BLOCKS.registerBlock("melter",
            MelterBlock::new,
            p -> p.sound(SoundType.METAL).strength(2.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<RadiationScrubberBlock> RADIATION_SCRUBBER = BLOCKS.registerBlock("radiation_scrubber",
            RadiationScrubberBlock::new,
            p -> p.sound(SoundType.METAL).strength(2.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> ROD_CONTROLLER = BLOCKS.registerBlock("rod_controller",
            Block::new,
            p -> p.sound(SoundType.METAL).strength(2.0f));

    public static final DeferredBlock<ReactorBuilderBlock> REACTOR_BUILDER = BLOCKS.registerBlock("reactor_builder",
            ReactorBuilderBlock::new,
            p -> p.sound(SoundType.METAL).strength(2.0f));

    // Resource ores (stone + deepslate)
    public static final DeferredBlock<Block> URANIUM_ORE = BLOCKS.registerBlock("uranium_ore",
            Block::new,
            p -> p.sound(SoundType.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE = BLOCKS.registerBlock("deep_uranium_ore",
            Block::new,
            p -> p.sound(SoundType.DEEPSLATE).strength(4.5f, 3.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> LEAD_ORE = BLOCKS.registerBlock("lead_ore",
            Block::new,
            p -> p.sound(SoundType.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DEEPSLATE_LEAD_ORE = BLOCKS.registerBlock("deep_lead_ore",
            Block::new,
            p -> p.sound(SoundType.DEEPSLATE).strength(4.5f, 3.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> BORON_ORE = BLOCKS.registerBlock("boron_ore",
            Block::new,
            p -> p.sound(SoundType.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DEEPSLATE_BORON_ORE = BLOCKS.registerBlock("deep_boron_ore",
            Block::new,
            p -> p.sound(SoundType.DEEPSLATE).strength(4.5f, 3.0f).requiresCorrectToolForDrops());

    // Storage and raw blocks
    public static final DeferredBlock<Block> URANIUM_BLOCK = BLOCKS.registerBlock("uranium_block",
            Block::new,
            p -> p.sound(SoundType.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> URANIUM_RAW_BLOCK = BLOCKS.registerBlock("uranium_raw_block",
            Block::new,
            p -> p.sound(SoundType.STONE).strength(5.0f, 6.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> LEAD_BLOCK = BLOCKS.registerBlock("lead_block",
            Block::new,
            p -> p.sound(SoundType.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> RAW_LEAD_BLOCK = BLOCKS.registerBlock("raw_lead_block",
            Block::new,
            p -> p.sound(SoundType.STONE).strength(5.0f, 6.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> BORON_BLOCK = BLOCKS.registerBlock("boron_block",
            Block::new,
            p -> p.sound(SoundType.COPPER).strength(5.0f, 6.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> RAW_BORON_BLOCK = BLOCKS.registerBlock("raw_boron_block",
            Block::new,
            p -> p.sound(SoundType.COPPER).strength(5.0f, 6.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> GRAPHITE_BLOCK = BLOCKS.registerBlock("graphite_block",
            Block::new,
            p -> p.sound(SoundType.STONE).strength(5.0f, 6.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> AZURITE_BLOCK = BLOCKS.registerBlock("azurite_block",
            Block::new,
            p -> p.sound(SoundType.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> TOUGH_ALLOY_BLOCK = BLOCKS.registerBlock("tough_alloy_block",
            Block::new,
            p -> p.sound(SoundType.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops());

    static {
        for (Identifier coilId : HeatingCoilRegistry.getBuiltinCoilIds()) {
            String path = coilId.getPath();
            HEATING_COIL_BLOCKS.add(BLOCKS.registerBlock(path + "_off",
                    props -> new HeatingCoilBlock(props, coilId, false),
                    p -> p.sound(SoundType.METAL).strength(2.0f).requiresCorrectToolForDrops()));
            HEATING_COIL_BLOCKS.add(BLOCKS.registerBlock(path + "_on",
                    props -> new HeatingCoilBlock(props, coilId, true),
                    p -> p.sound(SoundType.METAL).strength(2.0f).requiresCorrectToolForDrops()));
        }
    }

    /** Returns the heating coil block for the given id and on state, or null. */
    public static Block getHeatingCoilBlock(Identifier coilId, boolean isOn) {
        for (DeferredBlock<HeatingCoilBlock> db : HEATING_COIL_BLOCKS) {
            HeatingCoilBlock b = db.get();
            if (b.getCoilId().equals(coilId) && b.isOn() == isOn) return b;
        }
        return null;
    }

    private ModBlocks() {}
}
