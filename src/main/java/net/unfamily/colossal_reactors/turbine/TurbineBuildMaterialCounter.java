package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.item.ModItems;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estimates materials for turbine builder preview.
 */
public final class TurbineBuildMaterialCounter {

    public record MaterialCount(ItemStack stack, int count) {}

    private TurbineBuildMaterialCounter() {}

    public static Map<String, MaterialCount> estimate(
            RegistryAccess registryAccess,
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int rodPattern, int coilIndex, int coilLayerCount) {

        int w = sizeLeft + sizeRight + 1;
        int h = sizeHeight;
        int d = sizeDepth;
        int volume = w * h * d;
        int shell = 2 * (w * d + w * h + d * h);
        int interior = Math.max(0, volume - shell);

        TurbineSimulation.SimulationResult sim = TurbineSimulation.simulateFromBuilderParams(
                registryAccess, sizeLeft, sizeRight, sizeHeight, sizeDepth, rodPattern, coilIndex, coilLayerCount);

        Map<String, MaterialCount> out = new LinkedHashMap<>();
        out.put("casing", new MaterialCount(new ItemStack(ModBlocks.TURBINE_CASING.get()), shell));
        out.put("rod", new MaterialCount(new ItemStack(ModBlocks.TURBINE_ROD.get()), sim.rodColumns() * Math.max(1, h - 2)));
        out.put("blade", new MaterialCount(new ItemStack(ModItems.TURBINE_BLADE.get()), sim.bladeCount()));
        out.put("rod_controller", new MaterialCount(new ItemStack(ModBlocks.TURBINE_ROD_CONTROLLER.get()), sim.rodColumns()));
        int coilBlocks = sim.coilBlockCount();
        if (coilIndex >= 0 && coilIndex < ElecCoilLoader.getAllDefinitions().size()) {
            var def = ElecCoilLoader.getAllDefinitions().get(coilIndex);
            ItemStack coilStack = coilStackFor(registryAccess, def);
            out.put("coil", new MaterialCount(coilStack, coilBlocks));
        }
        return out;
    }

    private static ItemStack coilStackFor(RegistryAccess registryAccess, ElecCoilDefinition def) {
        if (!def.validBlocks().isEmpty()) {
            String sel = def.validBlocks().getFirst();
            if (!sel.startsWith("#")) {
                var id = net.minecraft.resources.ResourceLocation.tryParse(sel);
                if (id != null) {
                    var block = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.BLOCK).get(id);
                    if (block != null) {
                        return new ItemStack(block);
                    }
                }
            }
        }
        return new ItemStack(ModBlocks.TURBINE_CASING.get());
    }
}
