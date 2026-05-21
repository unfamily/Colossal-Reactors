package net.unfamily.colossal_reactors.gas;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Handle for a registered gas type (fluids, block, bucket).
 */
public final class RegisteredGas {
    private final String name;
    private final int tintArgb;
    private final DeferredHolder<Fluid, ? extends Fluid> sourceFluid;
    private final DeferredHolder<Fluid, ? extends Fluid> flowingFluid;
    private final DeferredHolder<Block, ? extends Block> block;
    private final DeferredHolder<Item, ? extends Item> bucketItem;
    private final ResourceLocation sourceFluidId;
    private final ResourceLocation blockId;
    private final ResourceLocation bucketId;

    public RegisteredGas(
            String name,
            int tintArgb,
            DeferredHolder<Fluid, ? extends Fluid> sourceFluid,
            DeferredHolder<Fluid, ? extends Fluid> flowingFluid,
            DeferredHolder<Block, ? extends Block> block,
            DeferredHolder<Item, ? extends Item> bucketItem,
            ResourceLocation sourceFluidId,
            ResourceLocation blockId,
            ResourceLocation bucketId
    ) {
        this.name = name;
        this.tintArgb = tintArgb;
        this.sourceFluid = sourceFluid;
        this.flowingFluid = flowingFluid;
        this.block = block;
        this.bucketItem = bucketItem;
        this.sourceFluidId = sourceFluidId;
        this.blockId = blockId;
        this.bucketId = bucketId;
    }

    public String name() {
        return name;
    }

    public int tintArgb() {
        return tintArgb;
    }

    public Fluid sourceFluid() {
        return sourceFluid.get();
    }

    public Fluid flowingFluid() {
        return flowingFluid.get();
    }

    public DeferredHolder<Block, ? extends Block> blockHolder() {
        return block;
    }

    public Block block() {
        return block.get();
    }

    public Item bucketItem() {
        return bucketItem.get();
    }

    public ResourceLocation sourceFluidId() {
        return sourceFluidId;
    }

    public ResourceLocation blockId() {
        return blockId;
    }

    public ResourceLocation bucketId() {
        return bucketId;
    }
}
