package net.unfamily.colossal_reactors.data;

import com.mojang.serialization.MapCodec;
import net.unfamily.colossal_reactors.Config;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Recipe/data condition: true when Config ENABLE_RADIATION_MANAGEMENT is true.
 * Used so radiation scrubber and radiation cure recipes load only when the dev flag is on (and Mekanism is loaded).
 */
public enum RadiationManagementCondition implements ICondition {
    INSTANCE;

    public static final MapCodec<RadiationManagementCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(IContext context) {
        return Config.ENABLE_RADIATION_MANAGEMENT.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
