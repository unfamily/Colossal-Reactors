package net.unfamily.colossal_reactors.integration.mekanism;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Parses datapack selectors: {@code #tag/id} (item or fluid), {@code %chemical} (Mek), or plain registry id.
 */
public final class MaterialSelector {

    public enum Kind {
        ITEM_TAG,
        ITEM_ID,
        FLUID_TAG,
        FLUID_ID,
        CHEMICAL_TAG,
        CHEMICAL_ID
    }

    public record Parsed(Kind kind, ResourceLocation id) {}

    private MaterialSelector() {}

    public static boolean isChemicalPrefix(String selector) {
        return selector != null && selector.startsWith("%");
    }

    public static boolean isTagPrefix(String selector) {
        return selector != null && selector.startsWith("#");
    }

    @Nullable
    public static Parsed parse(String selector) {
        if (selector == null || selector.isBlank()) return null;
        if (selector.startsWith("%")) {
            ResourceLocation id = ResourceLocation.tryParse(selector.substring(1));
            if (id == null) return null;
            if (id.getNamespace().equals("mekanism") && MekChemicalHelper.chemicalTagExists(id)) {
                return new Parsed(Kind.CHEMICAL_TAG, id);
            }
            return new Parsed(Kind.CHEMICAL_ID, id);
        }
        if (selector.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(selector.substring(1));
            if (id == null) return null;
            TagKey<Fluid> fluidTag = TagKey.create(Registries.FLUID, id);
            if (BuiltInRegistries.FLUID.getTag(fluidTag).isPresent()) {
                return new Parsed(Kind.FLUID_TAG, id);
            }
            return new Parsed(Kind.ITEM_TAG, id);
        }
        ResourceLocation id = ResourceLocation.tryParse(selector);
        if (id == null) return null;
        if (BuiltInRegistries.FLUID.containsKey(id) && BuiltInRegistries.FLUID.get(id) != Fluids.EMPTY) {
            return new Parsed(Kind.FLUID_ID, id);
        }
        return new Parsed(Kind.ITEM_ID, id);
    }

    public static boolean matchesItem(ItemStack stack, String selector) {
        if (stack == null || stack.isEmpty()) return false;
        Parsed p = parse(selector);
        if (p == null) return false;
        Item item = stack.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        return switch (p.kind()) {
            case ITEM_ID -> p.id().equals(itemId);
            case ITEM_TAG -> {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, p.id());
                yield stack.is(tag);
            }
            default -> false;
        };
    }

    public static boolean matchesFluid(Fluid fluid, String selector) {
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        Parsed p = parse(selector);
        if (p == null) return false;
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        return switch (p.kind()) {
            case FLUID_ID -> p.id().equals(fluidId);
            case FLUID_TAG -> {
                TagKey<Fluid> tag = TagKey.create(Registries.FLUID, p.id());
                yield fluid.is(tag);
            }
            default -> false;
        };
    }

    public static boolean matchesFluidStack(FluidStack stack, String selector) {
        return stack != null && !stack.isEmpty() && matchesFluid(stack.getFluid(), selector);
    }

    /** Mek chemical stack match (reflection). */
    public static boolean matchesChemical(Object chemicalStack, String selector) {
        if (!ModList.get().isLoaded("mekanism")) return false;
        return MekChemicalHelper.matchesSelector(chemicalStack, selector);
    }

    public static boolean isFluidSelector(String selector) {
        Parsed p = parse(selector);
        return p != null && (p.kind() == Kind.FLUID_ID || p.kind() == Kind.FLUID_TAG);
    }

    public static boolean isItemSelector(String selector) {
        Parsed p = parse(selector);
        return p != null && (p.kind() == Kind.ITEM_ID || p.kind() == Kind.ITEM_TAG);
    }

    public static boolean isChemicalSelector(String selector) {
        Parsed p = parse(selector);
        return p != null && (p.kind() == Kind.CHEMICAL_ID || p.kind() == Kind.CHEMICAL_TAG);
    }
}
