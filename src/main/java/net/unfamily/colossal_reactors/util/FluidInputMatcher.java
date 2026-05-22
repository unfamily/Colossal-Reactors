package net.unfamily.colossal_reactors.util;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Matches datapack fluid selectors ({@code #tag}, registry id). */
public final class FluidInputMatcher {

    private FluidInputMatcher() {}

    public static boolean isChemicalPrefix(String selector) {
        return selector != null && selector.startsWith("%");
    }

    public static boolean matchesFluid(Fluid fluid, String selector) {
        if (fluid == null || fluid == Fluids.EMPTY || selector == null || selector.isBlank()) {
            return false;
        }
        if (isChemicalPrefix(selector)) {
            return false;
        }
        if (selector.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(selector.substring(1));
            if (tagId == null) {
                return false;
            }
            TagKey<Fluid> tag = TagKey.create(Registries.FLUID, tagId);
            return fluid.is(tag);
        }
        Identifier id = Identifier.tryParse(selector);
        return id != null && fluidIdMatches(fluid, id);
    }

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

    @Nullable
    public static Fluid resolvePreferredBufferFluid(List<String> inputs, RegistryAccess registryAccess) {
        if (inputs == null || registryAccess == null) {
            return null;
        }
        for (String input : inputs) {
            if (input == null || input.isBlank() || isChemicalPrefix(input) || input.startsWith("#")) {
                continue;
            }
            Identifier id = Identifier.tryParse(input);
            if (id == null) {
                continue;
            }
            Fluid f = BuiltInRegistries.FLUID.getValue(id);
            if (f != null && f != Fluids.EMPTY) {
                return f;
            }
        }
        for (String input : inputs) {
            if (input == null || input.isBlank() || isChemicalPrefix(input) || !input.startsWith("#")) {
                continue;
            }
            Identifier tagId = Identifier.tryParse(input.substring(1));
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

    private static boolean fluidIdMatches(Fluid fluid, Identifier expectedId) {
        Identifier fluidId = BuiltInRegistries.FLUID.getKey(fluid);
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
}
