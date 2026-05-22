package net.unfamily.colossal_reactors.integration.mekanism;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        return switch (p.kind()) {
            case FLUID_ID -> fluidIdMatches(fluid, p.id());
            case FLUID_TAG -> {
                TagKey<Fluid> tag = TagKey.create(Registries.FLUID, p.id());
                yield fluid.is(tag);
            }
            default -> false;
        };
    }

    /**
     * Preferred liquid for internal buffers when storing chemical pulls (explicit fluid ids before tags).
     */
    @Nullable
    public static Fluid resolvePreferredBufferFluid(List<String> inputs, RegistryAccess registryAccess) {
        if (inputs == null || registryAccess == null) {
            return null;
        }
        for (String input : inputs) {
            if (input == null || input.isBlank() || isChemicalPrefix(input)) {
                continue;
            }
            Parsed p = parse(input);
            if (p != null && p.kind() == Kind.FLUID_ID) {
                Fluid f = BuiltInRegistries.FLUID.get(p.id());
                if (f != null && f != Fluids.EMPTY) {
                    return f;
                }
            }
        }
        for (String input : inputs) {
            if (input == null || input.isBlank() || isChemicalPrefix(input)) {
                continue;
            }
            if (!input.startsWith("#")) {
                continue;
            }
            ResourceLocation tagId = ResourceLocation.tryParse(input.substring(1));
            if (tagId == null) {
                continue;
            }
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
            var holders = registryAccess.lookup(Registries.FLUID).flatMap(l -> l.get(tagKey)).orElse(null);
            if (holders == null) {
                continue;
            }
            for (Holder<Fluid> holder : holders) {
                Fluid f = holder.value();
                if (f != null && f != Fluids.EMPTY) {
                    return f;
                }
            }
        }
        return null;
    }

    /** True when coolant definition inputs overlap simulation selectors (same list or shared entry). */
    public static boolean coolantInputsMatchSelectors(List<String> defInputs, List<String> selectors) {
        if (defInputs == null || selectors == null) {
            return false;
        }
        if (defInputs.equals(selectors)) {
            return true;
        }
        for (String input : defInputs) {
            if (input != null && selectors.contains(input)) {
                return true;
            }
        }
        return false;
    }

    /** True when {@code fluid} matches any liquid selector in {@code inputs} (skips chemical {@code %} entries). */
    public static boolean matchesAnyFluidInput(Fluid fluid, List<String> inputs) {
        if (fluid == null || fluid == Fluids.EMPTY || inputs == null) {
            return false;
        }
        for (String input : inputs) {
            if (input == null || input.isBlank() || isChemicalPrefix(input)) {
                continue;
            }
            if (matchesFluid(fluid, input)) {
                return true;
            }
        }
        return false;
    }

    /** Registry id match; flowing fluids also match their source id (e.g. steam / steam_flowing). */
    private static boolean fluidIdMatches(Fluid fluid, ResourceLocation expectedId) {
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (expectedId.equals(fluidId)) {
            return true;
        }
        if (fluid instanceof FlowingFluid flowing) {
            Fluid source = flowing.getSource();
            if (source != null && source != Fluids.EMPTY) {
                return expectedId.equals(BuiltInRegistries.FLUID.getKey(source));
            }
        }
        return false;
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
