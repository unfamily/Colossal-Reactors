package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.unfamily.colossal_reactors.turbine.TurbineRodControllerLayout.Center;

/**
 * Maps turbine builder placement axis to world positions for rods, closure deck, and coil zone.
 */
public final class TurbineRotorLayout {

    private final Direction growthAxis;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final int crossSizeA;
    private final int crossSizeB;
    private final int rodExtent;
    private final int coilStartInterior;
    private final int effectiveCoilLayers;
    private final int startCoord;
    private final int step;

    private TurbineRotorLayout(
            Direction growthAxis,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            int crossSizeA, int crossSizeB,
            int rodExtent, int coilStartInterior, int effectiveCoilLayers,
            int startCoord, int step) {
        this.growthAxis = growthAxis;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.crossSizeA = crossSizeA;
        this.crossSizeB = crossSizeB;
        this.rodExtent = rodExtent;
        this.coilStartInterior = coilStartInterior;
        this.effectiveCoilLayers = effectiveCoilLayers;
        this.startCoord = startCoord;
        this.step = step;
    }

    public static TurbineRotorLayout from(
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            int w, int h, int d, int requestedCoilLayers, Direction growthAxis) {
        int interiorAlong = interiorExtentAlong(growthAxis, w, h, d);
        int coils = TurbineRodSpaceLayout.coilLayerCount(interiorAlong, requestedCoilLayers);
        int extent = Math.max(0, interiorAlong - coils);
        int coilStart = Math.max(0, interiorAlong - coils);
        int start;
        int stepDir;
        switch (growthAxis) {
            case UP -> {
                start = minY + 1;
                stepDir = 1;
            }
            case DOWN -> {
                start = maxY - 1;
                stepDir = -1;
            }
            case SOUTH -> {
                start = minZ + 1;
                stepDir = 1;
            }
            case NORTH -> {
                start = maxZ - 1;
                stepDir = -1;
            }
            case EAST -> {
                start = minX + 1;
                stepDir = 1;
            }
            case WEST -> {
                start = maxX - 1;
                stepDir = -1;
            }
            default -> {
                start = minY + 1;
                stepDir = 1;
            }
        }
        int crossA;
        int crossB;
        switch (growthAxis.getAxis()) {
            case Y -> {
                crossA = TurbineRodSpaceLayout.interiorWidth(w);
                crossB = TurbineRodSpaceLayout.interiorDepth(d);
            }
            case Z -> {
                crossA = TurbineRodSpaceLayout.interiorWidth(w);
                crossB = TurbineRodSpaceLayout.interiorHeight(h);
            }
            case X -> {
                crossA = TurbineRodSpaceLayout.interiorHeight(h);
                crossB = TurbineRodSpaceLayout.interiorDepth(d);
            }
            default -> {
                crossA = 0;
                crossB = 0;
            }
        }
        return new TurbineRotorLayout(
                growthAxis, minX, minY, minZ, maxX, maxY, maxZ,
                crossA, crossB, extent, coilStart, coils, start, stepDir);
    }

    private static int interiorExtentAlong(Direction growthAxis, int w, int h, int d) {
        return switch (growthAxis.getAxis()) {
            case Y -> TurbineRodSpaceLayout.interiorHeight(h);
            case Z -> TurbineRodSpaceLayout.interiorDepth(d);
            case X -> TurbineRodSpaceLayout.interiorWidth(w);
            default -> TurbineRodSpaceLayout.interiorHeight(h);
        };
    }

    public Direction growthAxis() {
        return growthAxis;
    }

    public int crossSizeA() {
        return crossSizeA;
    }

    public int crossSizeB() {
        return crossSizeB;
    }

    public int rodExtent() {
        return rodExtent;
    }

    public int coilStartInterior() {
        return coilStartInterior;
    }

    public int closureCoord() {
        return startCoord + rodExtent * step;
    }

    public int coordOnAxis(int layerIndex) {
        return startCoord + layerIndex * step;
    }

    public int interiorExtentAlong() {
        return coilStartInterior + rodExtent;
    }

    public int layerIndexFromWorldCoord(int worldCoord) {
        if (step > 0) {
            return (worldCoord - startCoord) / step;
        }
        return (startCoord - worldCoord) / (-step);
    }

    public boolean isInteriorLayerIndex(int layerIndex) {
        return layerIndex >= 0 && layerIndex < interiorExtentAlong();
    }

    public Center primaryCenter() {
        return TurbineRodControllerLayout.bestPrimaryCenter(crossSizeA, crossSizeB);
    }

    public BlockPos controllerPos(Center center) {
        return rodPos(rodExtent, center.rx(), center.rz());
    }

    public BlockPos rodPos(int layerIndex, int crossA, int crossB) {
        int axisCoord = coordOnAxis(layerIndex);
        return switch (growthAxis.getAxis()) {
            case Y -> new BlockPos(
                    TurbineRodControllerLayout.closureWorldX(minX, crossA),
                    axisCoord,
                    TurbineRodControllerLayout.closureWorldZ(minZ, crossB));
            case Z -> new BlockPos(
                    TurbineRodControllerLayout.closureWorldX(minX, crossA),
                    minY + 1 + crossB,
                    axisCoord);
            case X -> new BlockPos(
                    axisCoord,
                    minY + 1 + crossA,
                    TurbineRodControllerLayout.closureWorldZ(minZ, crossB));
            default -> BlockPos.ZERO;
        };
    }

    public int coilLoopStart() {
        return coordOnAxis(coilStartInterior);
    }

    public int coilLoopEndExclusive() {
        return coordOnAxis(coilStartInterior + effectiveCoilLayers);
    }

    public int coilLoopStep() {
        return step;
    }

    public boolean isRodControllerAt(int x, int y, int z, Center center) {
        BlockPos p = controllerPos(center);
        return x == p.getX() && y == p.getY() && z == p.getZ();
    }

    public boolean isInRodZone(int x, int y, int z) {
        int coord = axisCoord(x, y, z);
        if (step > 0) {
            return coord >= startCoord && coord < closureCoord();
        }
        return coord <= startCoord && coord > closureCoord();
    }

    public int crossAFromWorld(int x, int y, int z) {
        return switch (growthAxis.getAxis()) {
            case Y, Z -> TurbineRodControllerLayout.worldToRodSpaceX(minX, x);
            case X -> y - minY - 1;
            default -> 0;
        };
    }

    public int crossBFromWorld(int x, int y, int z) {
        return switch (growthAxis.getAxis()) {
            case Y -> TurbineRodControllerLayout.worldToRodSpaceZ(minZ, z);
            case Z -> y - minY - 1;
            case X -> TurbineRodControllerLayout.worldToRodSpaceZ(minZ, z);
            default -> 0;
        };
    }

    public int interiorIndexFromWorld(int x, int y, int z) {
        return layerIndexFromWorldCoord(worldAxisCoord(x, y, z));
    }

    public boolean isCoilZoneWorld(int x, int y, int z) {
        int idx = interiorIndexFromWorld(x, y, z);
        return isInteriorLayerIndex(idx) && idx >= coilStartInterior;
    }

    public int worldAxisCoord(int x, int y, int z) {
        return axisCoord(x, y, z);
    }

    private int axisCoord(int x, int y, int z) {
        return switch (growthAxis.getAxis()) {
            case Y -> y;
            case Z -> z;
            case X -> x;
            default -> y;
        };
    }
}
