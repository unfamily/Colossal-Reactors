package net.unfamily.colossal_reactors.compat.rei;

import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;

public class ColossalReiCommonPlugin implements REICommonPlugin {

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(ColossalReiPlugin.EPHEMERAL_SERIALIZER_ID, ColossalReiSerializers.EPHEMERAL);
    }
}
