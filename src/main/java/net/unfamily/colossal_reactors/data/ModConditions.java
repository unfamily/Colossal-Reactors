package net.unfamily.colossal_reactors.data;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.unfamily.colossal_reactors.ColossalReactors;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

public final class ModConditions {

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ColossalReactors.MODID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<RadiationManagementCondition>> RADIATION_MANAGEMENT_ENABLED =
            CONDITION_CODECS.register("radiation_management_enabled", () -> RadiationManagementCondition.CODEC);

    private ModConditions() {}
}
