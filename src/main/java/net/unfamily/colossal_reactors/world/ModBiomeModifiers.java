package net.unfamily.colossal_reactors.world;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.unfamily.colossal_reactors.ColossalReactors;

public final class ModBiomeModifiers {

    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.world.BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, ColossalReactors.MODID);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.world.BiomeModifier>, com.mojang.serialization.MapCodec<AddFeaturesIfOregenEnabledModifier>> ADD_FEATURES_IF_OREGEN_ENABLED =
            BIOME_MODIFIER_SERIALIZERS.register("add_features_if_oregen_enabled", () -> AddFeaturesIfOregenEnabledModifier.CODEC);

    private ModBiomeModifiers() {}
}
