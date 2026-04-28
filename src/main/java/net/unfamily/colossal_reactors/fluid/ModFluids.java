package net.unfamily.colossal_reactors.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.BreeziumBlock;
import net.unfamily.colossal_reactors.block.EnderGooBlock;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.item.ModItems;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;

/**
 * Registers fluid types and fluids. Molten metals use custom block/fluid textures with tint.
 * Lead/uranium molten fluids are provided by Synergy; we only add molten_tough_alloy and gelid_breezium.
 */
public final class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES, ColossalReactors.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, ColossalReactors.MODID);

    /** Molten tough alloy (Synergy does not provide this). Uses custom molten textures. */
    public static final TintedFluid MOLTEN_TOUGH_ALLOY = registerMolten("molten_tough_alloy", 0xFF5A6A7A,
            "fluid.colossal_reactors.molten_tough_alloy");

    /** Ender goo: teleports entities on contact. Uses custom EnderGooBlock. */
    public static final TintedFluid ENDER_GOO = registerEnderGoo();

    /** Gelid breezium: gravity, 3x3 snow, freezes water, cold damage. Uses custom BreeziumBlock. */
    public static final TintedFluid GELID_BREEZIUM = registerGelidBreezium();

    private ModFluids() {}

    /**
     * Registers a molten metal fluid with custom still/flow textures and ARGB tint.
     * Hot (1300), high viscosity, not swimmable.
     */
    private static TintedFluid registerMolten(String name, int tintColor, String descriptionId) {
        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register(name + "_type",
                () -> new FluidType(FluidType.Properties.create()
                        .descriptionId(descriptionId)
                        .lightLevel(7)
                        .temperature(1300)
                        .viscosity(6000)
                        .canDrown(false)
                        .canSwim(false)
                        .canPushEntity(true)
                        .canConvertToSource(false)
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

        return registerTintedFluid(name, type, 7, false);
    }

    /**
     * Gelid breezium: water-like fluid with bright cyan tint. Cold, swimmable.
     */
    private static TintedFluid registerGelidBreezium() {
        int tintColor = 0xFF00E5FF; // bright cyan

        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register("gelid_breezium_type",
                () -> new FluidType(FluidType.Properties.create()
                        .descriptionId("fluid.colossal_reactors.gelid_breezium")
                        .lightLevel(0)
                        .temperature(260)
                        .viscosity(1000)
                        .canDrown(true)
                        .canSwim(true)
                        .canPushEntity(true)
                        .canConvertToSource(false)
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

        return registerTintedFluid("gelid_breezium", type, 0, true, BreeziumBlock::new);
    }

    /** Ender goo: same visuals as molten, custom block for teleport-on-contact (handled in SpecialFluidEffects). */
    private static TintedFluid registerEnderGoo() {
        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register("ender_goo_type",
                () -> new FluidType(FluidType.Properties.create()
                        .descriptionId("fluid.colossal_reactors.ender_goo")
                        .lightLevel(7)
                        .temperature(1300)
                        .viscosity(6000)
                        .canDrown(false)
                        .canSwim(false)
                        .canPushEntity(true)
                        .canConvertToSource(false)
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
        return registerTintedFluid("ender_goo", type, 7, false, EnderGooBlock::new);
    }

    /**
     * Shared registration for a tinted fluid (block, bucket, source, flowing).
     * Uses custom block class when blockFactory is provided (e.g. BreeziumBlock, EnderGooBlock).
     */
    private static TintedFluid registerTintedFluid(String name, DeferredHolder<FluidType, FluidType> type, int blockLightLevel, boolean sourceIdIsBaseName, BiFunction<FlowingFluid, BlockBehaviour.Properties, LiquidBlock> blockFactory) {
        var refs = new Object() {
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
            DeferredHolder<Fluid, FlowingFluid> flowing;
            DeferredBlock<Block> block;
            DeferredHolder<Item, BucketItem> bucket;
        };

        BlockBehaviour.Properties blockProps = BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .replaceable()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .lightLevel(s -> blockLightLevel);

        BaseFlowingFluid.Properties prop = new BaseFlowingFluid.Properties(
                        type,
                        () -> refs.source.get(),
                        () -> refs.flowing.get())
                .block(() -> (LiquidBlock) refs.block.get())
                .bucket(() -> refs.bucket.get());

        String sourceId = sourceIdIsBaseName ? name : (name + "_source");
        refs.source = FLUIDS.register(sourceId, () -> new BaseFlowingFluid.Source(prop));
        refs.flowing = FLUIDS.register(name + "_flowing", () -> new BaseFlowingFluid.Flowing(prop));
        refs.block = ModBlocks.BLOCKS.register(name,
                () -> blockFactory.apply(refs.flowing.get(), blockProps));
        refs.bucket = ModItems.ITEMS.register(name + "_bucket",
                () -> new BucketItem(refs.source.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

        return new TintedFluid(refs.source, refs.flowing, refs.block, refs.bucket);
    }

    /**
     * Shared registration for a tinted fluid with default LiquidBlock.
     */
    private static TintedFluid registerTintedFluid(String name, DeferredHolder<FluidType, FluidType> type, int blockLightLevel, boolean sourceIdIsBaseName) {
        return registerTintedFluid(name, type, blockLightLevel, sourceIdIsBaseName, LiquidBlock::new);
    }

    public record TintedFluid(
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, FlowingFluid> flowing,
            DeferredBlock<Block> block,
            DeferredHolder<Item, BucketItem> bucket
    ) {
        public Fluid getSource() { return source.get(); }
        public Fluid getFlowing() { return flowing.get(); }
        public Block getBlock() { return block.get(); }
    }
}
