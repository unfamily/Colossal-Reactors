package net.unfamily.colossal_reactors.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nonnull;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.unfamily.colossal_reactors.Config;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Biome modifier that adds placed features to biomes only when ore generation for the given ore type is not disabled by config.
 * JSON: biomes, features, step, ore ("uranium" | "lead" | "boron").
 */
public record AddFeaturesIfOregenEnabledModifier(
        HolderSet<Biome> biomes,
        HolderSet<PlacedFeature> features,
        GenerationStep.Decoration step,
        String ore) implements BiomeModifier {

    public static final MapCodec<AddFeaturesIfOregenEnabledModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(AddFeaturesIfOregenEnabledModifier::biomes),
                    PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(AddFeaturesIfOregenEnabledModifier::features),
                    GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(AddFeaturesIfOregenEnabledModifier::step),
                    com.mojang.serialization.Codec.STRING.fieldOf("ore").forGetter(AddFeaturesIfOregenEnabledModifier::ore)
            ).apply(instance, AddFeaturesIfOregenEnabledModifier::new)
    );

    private boolean isOregenDisabled() {
        return switch (ore) {
            case "uranium" -> Config.DISABLE_URANIUM_OREGEN.get();
            case "lead" -> Config.DISABLE_LEAD_OREGEN.get();
            case "boron" -> Config.DISABLE_BORON_OREGEN.get();
            default -> false;
        };
    }

    @Override
    public void modify(@Nonnull Holder<Biome> biome, @Nonnull Phase phase, @Nonnull ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (isOregenDisabled()) {
            return;
        }
        if (phase != Phase.ADD) {
            return;
        }
        if (!biomes.contains(biome)) {
            return;
        }
        var generation = builder.getGenerationSettings();
        for (Holder<PlacedFeature> feature : features) {
            generation.addFeature(step, feature); // step and feature are non-null from record
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
