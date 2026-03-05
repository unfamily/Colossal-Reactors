package net.unfamily.colossal_reactors.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.BlockAndTintGetter;
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
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.item.ModItems;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/**
 * Registers fluid types and fluids. Molten metals use custom block/fluid textures with tint.
 * Lead/uranium molten fluids are provided by Synergy; we only add molten_tough_alloy and gelid_breezium.
 */
public final class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES, ColossalReactors.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, ColossalReactors.MODID);

    /** Custom molten texture path (assets/.../textures/block/fluid/still.png and flow.png) */
    private static final ResourceLocation MOLTEN_STILL = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "block/fluid/still");
    private static final ResourceLocation MOLTEN_FLOW = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "block/fluid/flow");
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");

    /** Molten tough alloy (Synergy does not provide this). Uses custom molten textures. */
    public static final TintedFluid MOLTEN_TOUGH_ALLOY = registerMolten("molten_tough_alloy", 0xFF5A6A7A,
            "fluid.colossal_reactors.molten_tough_alloy");

    /** Gelid breezium: vanilla water recolored bright cyan, cold coolant. */
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
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
                    @Override
                    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new IClientFluidTypeExtensions() {
                            @Override
                            public ResourceLocation getStillTexture() { return MOLTEN_STILL; }
                            @Override
                            public ResourceLocation getFlowingTexture() { return MOLTEN_FLOW; }
                            @Override
                            public ResourceLocation getOverlayTexture() { return WATER_OVERLAY; }
                            @Override
                            public int getTintColor() { return tintColor; }
                            @Override
                            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                                return tintColor;
                            }
                        });
                    }
                });

        return registerTintedFluid(name, type, 7);
    }

    /**
     * Gelid breezium: water-like fluid with bright cyan tint. Cold, swimmable.
     */
    private static TintedFluid registerGelidBreezium() {
        ResourceLocation stillTex = ResourceLocation.withDefaultNamespace("block/water_still");
        ResourceLocation flowingTex = ResourceLocation.withDefaultNamespace("block/water_flow");
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
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
                    @Override
                    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new IClientFluidTypeExtensions() {
                            @Override
                            public ResourceLocation getStillTexture() { return stillTex; }
                            @Override
                            public ResourceLocation getFlowingTexture() { return flowingTex; }
                            @Override
                            public ResourceLocation getOverlayTexture() { return WATER_OVERLAY; }
                            @Override
                            public int getTintColor() { return tintColor; }
                            @Override
                            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                                return tintColor;
                            }
                        });
                    }
                });

        return registerTintedFluid("gelid_breezium", type, 0);
    }

    /**
     * Shared registration for a tinted fluid (block, bucket, source, flowing).
     */
    private static TintedFluid registerTintedFluid(String name, DeferredHolder<FluidType, FluidType> type, int blockLightLevel) {
        var refs = new Object() {
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
            DeferredHolder<Fluid, FlowingFluid> flowing;
            DeferredBlock<Block> block;
            DeferredHolder<Item, BucketItem> bucket;
        };

        BaseFlowingFluid.Properties prop = new BaseFlowingFluid.Properties(
                        type,
                        () -> refs.source.get(),
                        () -> refs.flowing.get())
                .block(() -> (LiquidBlock) refs.block.get())
                .bucket(() -> refs.bucket.get());

        refs.source = FLUIDS.register(name + "_source", () -> new BaseFlowingFluid.Source(prop));
        refs.flowing = FLUIDS.register(name + "_flowing", () -> new BaseFlowingFluid.Flowing(prop));
        refs.block = ModBlocks.BLOCKS.register(name,
                () -> new LiquidBlock(refs.flowing.get(), BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .replaceable()
                        .noCollission()
                        .strength(100.0F)
                        .pushReaction(PushReaction.DESTROY)
                        .noLootTable()
                        .liquid()
                        .lightLevel(s -> blockLightLevel)));
        refs.bucket = ModItems.ITEMS.register(name + "_bucket",
                () -> new BucketItem(refs.source.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

        return new TintedFluid(refs.source, refs.flowing, refs.block, refs.bucket);
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
