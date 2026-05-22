package net.unfamily.colossal_reactors.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.fluid.ModFluids;
import net.unfamily.colossal_reactors.item.ModItems;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * In-mod gas registration for Colossal Reactors 1.21.1 only (no {@code iska_lib}).
 * World representation is a rising {@link GasLiquidBlock}; fluids are for stacks/ports/JEI/pumps.
 */
public final class ModGases {
    public static final int STEAM_TINT = 0xFFE8F0F0;
    private static final ResourceLocation GAS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "block/gas");

    private static RegisteredGas steam;

    private ModGases() {}

    public static RegisteredGas steam() {
        return steam;
    }

    /** Call once during mod construction (before registry events). */
    public static void registerSteam() {
        if (steam != null) {
            return;
        }

        String name = "steam";
        int tint = STEAM_TINT;
        String fluidSourceId = "gas_fluid_" + name;
        String fluidFlowingId = fluidSourceId + "_flowing";
        String blockId = "gas_" + name;
        String bucketId = blockId + "_bucket";

        var refs = new Object() {
            DeferredHolder<FluidType, FluidType> fluidType;
            DeferredHolder<Fluid, GasFlowingFluid.Source> source;
            DeferredHolder<Fluid, GasFlowingFluid.Flowing> flowing;
            DeferredBlock<GasLiquidBlock> block;
            DeferredHolder<Item, GasBucketItem> bucket;
        };

        refs.fluidType = ModFluids.FLUID_TYPES.register(fluidSourceId + "_type", () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid.colossal_reactors.gas_fluid_" + name)
                .lightLevel(0)
                .density(-1000)
                .viscosity(100)
                .temperature(300)
                .canDrown(false)
                .canSwim(false)
                .canPushEntity(false)
                .canConvertToSource(false)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return GAS_TEXTURE;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return GAS_TEXTURE;
                    }

                    @Override
                    public int getTintColor() {
                        return tint;
                    }

                    @Override
                    public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                        return tint;
                    }

                    @Override
                    public boolean renderFluid(
                            FluidState fluidState,
                            BlockAndTintGetter getter,
                            BlockPos pos,
                            VertexConsumer vertexConsumer,
                            BlockState blockState
                    ) {
                        return GasRegistry.fromFluid(fluidState.getType()) != null;
                    }
                });
            }
        });

        AtomicReference<RegisteredGas> gasRef = new AtomicReference<>();

        BaseFlowingFluid.Properties fluidProps = new BaseFlowingFluid.Properties(
                refs.fluidType,
                () -> refs.source.get(),
                () -> refs.flowing.get())
                .block(() -> refs.block.get())
                .bucket(() -> refs.bucket.isBound() ? refs.bucket.get() : null);

        refs.source = ModFluids.FLUIDS.register(fluidSourceId, () -> new GasFlowingFluid.Source(fluidProps));
        refs.flowing = ModFluids.FLUIDS.register(fluidFlowingId, () -> new GasFlowingFluid.Flowing(fluidProps));

        refs.block = ModBlocks.BLOCKS.register(blockId, () -> new GasLiquidBlock(
                refs.source.get(),
                GasLiquidBlock.configureProperties(0),
                gasRef::get,
                GasLiquidBlock.DEFAULT_RISE_TICK_INTERVAL));

        refs.bucket = ModItems.ITEMS.register(bucketId, () -> new GasBucketItem(
                new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1),
                gasRef.get(),
                refs.source));

        ResourceLocation sourceFluidLoc = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, fluidSourceId);
        ResourceLocation flowingFluidLoc = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, fluidFlowingId);
        ResourceLocation blockLoc = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, blockId);
        ResourceLocation bucketLoc = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, bucketId);

        steam = new RegisteredGas(
                name,
                tint,
                refs.source,
                refs.flowing,
                refs.block,
                refs.bucket,
                sourceFluidLoc,
                blockLoc,
                bucketLoc);
        gasRef.set(steam);

        GasRegistry.register(steam);
        GasRegistry.registerFluidAlias(flowingFluidLoc, steam);
    }
}
