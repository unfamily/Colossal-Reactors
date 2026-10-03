package net.unfamily.colossal_reactors.compat.emi;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.heatingcoil.ConsumeOption;

final class HeatingCoilEmiHelper {
    private static final int FLUID_DISPLAY_AMOUNT_MB = 1000;

    private HeatingCoilEmiHelper() {}

    static List<FluidStack> fluidStacks(ConsumeOption.FluidRequirement fluidReq, RegistryAccess registryAccess) {
        List<FluidStack> out = new ArrayList<>();
        if (fluidReq.isTag()) {
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, fluidReq.tagOrId());
            registryAccess.lookup(Registries.FLUID).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> out.add(new FluidStack(h.value(), FLUID_DISPLAY_AMOUNT_MB)))));
        } else {
            Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(fluidReq.tagOrId());
            if (fluid != Fluids.EMPTY) {
                out.add(new FluidStack(fluid, FLUID_DISPLAY_AMOUNT_MB));
            }
        }
        return out;
    }

    static List<ItemStack> itemStacks(ConsumeOption.ItemRequirement itemReq, RegistryAccess registryAccess) {
        int count = Math.max(1, itemReq.activation());
        List<ItemStack> out = new ArrayList<>();
        if (itemReq.isTag()) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, itemReq.tagOrId());
            registryAccess.lookup(Registries.ITEM).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> out.add(new ItemStack(h.value(), count)))));
        } else {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(itemReq.tagOrId());
            if (item != Items.AIR) {
                out.add(new ItemStack(item, count));
            }
        }
        return out;
    }

    static List<ItemStack> burnables(Level level) {
        List<ItemStack> out = new ArrayList<>();
        level.registryAccess().lookupOrThrow(Registries.ITEM).listElements().forEach(holder -> {
            Item item = holder.value();
            if (item == Items.AIR) {
                return;
            }
            ItemStack stack = new ItemStack(item);
            if (stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING, level.fuelValues()) > 0) {
                out.add(stack);
            }
        });
        return out;
    }
}
