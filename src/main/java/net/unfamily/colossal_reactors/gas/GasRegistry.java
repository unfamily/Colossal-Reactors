package net.unfamily.colossal_reactors.gas;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GasRegistry {
    private static final Map<Block, RegisteredGas> BY_BLOCK = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RegisteredGas> BY_FLUID_ID = new ConcurrentHashMap<>();
    private static final List<RegisteredGas> ALL = Collections.synchronizedList(new ArrayList<>());

    private GasRegistry() {}

    static void register(RegisteredGas gas) {
        BY_BLOCK.put(gas.block(), gas);
        BY_FLUID_ID.put(gas.sourceFluidId(), gas);
        ALL.add(gas);
    }

    static void registerFluidAlias(ResourceLocation fluidId, RegisteredGas gas) {
        BY_FLUID_ID.put(fluidId, gas);
    }

    @Nullable
    public static RegisteredGas fromBlock(Block block) {
        return BY_BLOCK.get(block);
    }

    @Nullable
    public static RegisteredGas fromState(BlockState state) {
        return fromBlock(state.getBlock());
    }

    @Nullable
    public static RegisteredGas fromFluid(Fluid fluid) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
        return id != null ? BY_FLUID_ID.get(id) : null;
    }

    public static List<RegisteredGas> all() {
        return List.copyOf(ALL);
    }
}
