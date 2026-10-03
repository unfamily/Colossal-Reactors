package net.unfamily.colossal_reactors.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.unfamily.colossal_reactors.block.HeatingCoilBlock;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.heatingcoil.ConsumeOption;

/** Shared heating-coil ingredient resolution for JEI/EMI/REI (no viewer API). */
public final class HeatingCoilViewerHelper {
    private static final int FLUID_DISPLAY_AMOUNT_MB = 1000;

    private HeatingCoilViewerHelper() {}

    public static List<ItemStack> offCoilStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (DeferredBlock<HeatingCoilBlock> db : ModBlocks.HEATING_COIL_BLOCKS) {
            HeatingCoilBlock block = db.get();
            if (!block.isOn()) {
                out.add(new ItemStack(block));
            }
        }
        return out;
    }

    public static ItemStack coilStack(ResourceLocation coilId, boolean on) {
        if (coilId == null) {
            return ItemStack.EMPTY;
        }
        Block block = ModBlocks.getHeatingCoilBlock(coilId, on);
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    public static List<FluidStack> fluidStacks(ConsumeOption.FluidRequirement fluidReq, RegistryAccess registryAccess) {
        List<FluidStack> out = new ArrayList<>();
        if (fluidReq.isTag()) {
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, fluidReq.tagOrId());
            registryAccess.lookup(Registries.FLUID).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> out.add(new FluidStack(h.value(), FLUID_DISPLAY_AMOUNT_MB)))));
        } else {
            Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(fluidReq.tagOrId());
            if (fluid != null && fluid != Fluids.EMPTY) {
                out.add(new FluidStack(fluid, FLUID_DISPLAY_AMOUNT_MB));
            }
        }
        return out;
    }

    public static List<ItemStack> itemStacks(ConsumeOption.ItemRequirement itemReq, RegistryAccess registryAccess) {
        int count = Math.max(1, itemReq.activation());
        List<ItemStack> out = new ArrayList<>();
        if (itemReq.isTag()) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, itemReq.tagOrId());
            registryAccess.lookup(Registries.ITEM).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> out.add(new ItemStack(h.value(), count)))));
        } else {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemReq.tagOrId());
            if (item != null && item != Items.AIR) {
                out.add(new ItemStack(item, count));
            }
        }
        return out;
    }

    public static List<ItemStack> burnables(Level level) {
        List<ItemStack> out = new ArrayList<>();
        level.registryAccess().lookupOrThrow(Registries.ITEM).listElements().forEach(holder -> {
            Item item = holder.value();
            if (item == Items.AIR) {
                return;
            }
            ItemStack stack = new ItemStack(item);
            if (stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING) > 0) {
                out.add(stack);
            }
        });
        return out;
    }
}
