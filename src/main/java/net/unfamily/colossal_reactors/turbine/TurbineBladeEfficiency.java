package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Blade efficiency from ascending/descending layer blade counts along rod controller axis.
 */
public final class TurbineBladeEfficiency {

    private TurbineBladeEfficiency() {}

    /**
     * @param layerBladeCounts blade count per layer along axis (bottom to top)
     */
    public static double computeMultiplier(List<Integer> layerBladeCounts) {
        if (layerBladeCounts == null || layerBladeCounts.isEmpty()) {
            return 1.0;
        }
        double bonus = Config.TURBINE_BLADE_EFFICIENCY_BONUS_PER_ASCENDING_LAYER.get();
        double malus = Config.TURBINE_BLADE_EFFICIENCY_MALUS_PER_DESCENDING_LAYER.get();
        double mult = 1.0;
        boolean bonusBroken = false;
        for (int i = 1; i < layerBladeCounts.size(); i++) {
            int prev = layerBladeCounts.get(i - 1);
            int cur = layerBladeCounts.get(i);
            if (cur > prev) {
                if (!bonusBroken) {
                    mult += bonus;
                }
            } else if (cur < prev) {
                bonusBroken = true;
                mult -= malus;
            }
        }
        return Math.max(0.01, mult);
    }

    public static double computeFromBounds(Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                           Direction axis) {
        List<Integer> layerCounts = collectLayerBladeCounts(level, minX, minY, minZ, maxX, maxY, maxZ, axis);
        return computeMultiplier(layerCounts);
    }

    public static List<Integer> collectLayerBladeCounts(Level level, int minX, int minY, int minZ,
                                                        int maxX, int maxY, int maxZ, Direction axis) {
        List<Integer> counts = new ArrayList<>();
        if (axis.getAxis() == Direction.Axis.Y) {
            for (int y = minY + 1; y < maxY; y++) {
                counts.add(maxBladesOnLayer(level, minX, y, minZ, maxX, maxY, maxZ, axis));
            }
        } else if (axis.getAxis() == Direction.Axis.X) {
            for (int x = minX + 1; x < maxX; x++) {
                counts.add(maxBladesOnLayerX(level, x, minY, minZ, maxX, maxY, maxZ, axis));
            }
        } else {
            for (int z = minZ + 1; z < maxZ; z++) {
                counts.add(maxBladesOnLayerZ(level, minX, minY, z, maxX, maxY, maxZ, axis));
            }
        }
        if (counts.isEmpty()) {
            counts.add(0);
        }
        return counts;
    }

    private static int maxBladesOnLayer(Level level, int minX, int y, int minZ, int maxX, int maxY, int maxZ, Direction axis) {
        int max = 0;
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(p);
                if (state.is(ModBlocks.TURBINE_ROD.get()) && state.hasProperty(TurbineRodBlock.FACING)) {
                    Direction rodAxis = state.getValue(TurbineRodBlock.FACING);
                    if (rodAxis.getAxis() == axis.getAxis()) continue;
                    int blades = countBalancedBlades(level, p, rodAxis);
                    max = Math.max(max, blades);
                }
            }
        }
        return max;
    }

    private static int maxBladesOnLayerX(Level level, int x, int minY, int minZ, int maxX, int maxY, int maxZ, Direction axis) {
        int max = 0;
        for (int y = minY + 1; y < maxY; y++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(p);
                if (state.is(ModBlocks.TURBINE_ROD.get()) && state.hasProperty(TurbineRodBlock.FACING)) {
                    int blades = countBalancedBlades(level, p, state.getValue(TurbineRodBlock.FACING));
                    max = Math.max(max, blades);
                }
            }
        }
        return max;
    }

    private static int maxBladesOnLayerZ(Level level, int minX, int minY, int z, int maxX, int maxY, int maxZ, Direction axis) {
        int max = 0;
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(p);
                if (state.is(ModBlocks.TURBINE_ROD.get()) && state.hasProperty(TurbineRodBlock.FACING)) {
                    int blades = countBalancedBlades(level, p, state.getValue(TurbineRodBlock.FACING));
                    max = Math.max(max, blades);
                }
            }
        }
        return max;
    }

    private static int countBalancedBlades(Level level, BlockPos rodPos, Direction rodAxis) {
        int total = TurbineBladePlacement.totalBladesOnRod(level, rodPos, rodAxis);
        if (!Boolean.TRUE.equals(Config.TURBINE_REQUIRE_BALANCED_BLADE_RINGS.get())) {
            return total;
        }
        if (total % 4 != 0) return 0;
        return total;
    }
}
