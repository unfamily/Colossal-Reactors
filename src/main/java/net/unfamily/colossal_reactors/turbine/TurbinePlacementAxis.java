package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.Direction;

/**
 * Six rotor placement directions for turbine builder (rod + rod controller {@link net.unfamily.colossal_reactors.block.TurbineRodBlock#FACING}).
 * Labels use flow notation: D-U = down toward up.
 */
public enum TurbinePlacementAxis {

    DOWN_UP(Direction.UP, "down_up"),
    UP_DOWN(Direction.DOWN, "up_down"),
    NORTH_SOUTH(Direction.SOUTH, "north_south"),
    SOUTH_NORTH(Direction.NORTH, "south_north"),
    EAST_WEST(Direction.WEST, "east_west"),
    WEST_EAST(Direction.EAST, "west_east");

    /** Default: bottom-to-top rotor (D-U). */
    public static final int DEFAULT_INDEX = DOWN_UP.ordinal();

    private final Direction facing;
    private final String id;

    TurbinePlacementAxis(Direction facing, String id) {
        this.facing = facing;
        this.id = id;
    }

    public Direction facing() {
        return facing;
    }

    /** Lang suffix: {@code gui.colossal_reactors.turbine_builder.placement_axis.short.<id>}. */
    public String id() {
        return id;
    }

    public static int count() {
        return values().length;
    }

    public static TurbinePlacementAxis fromIndex(int index) {
        TurbinePlacementAxis[] all = values();
        if (index < 0 || index >= all.length) {
            return DOWN_UP;
        }
        return all[index];
    }

    public static TurbinePlacementAxis fromFacing(Direction facing) {
        if (facing == null) {
            return DOWN_UP;
        }
        for (TurbinePlacementAxis axis : values()) {
            if (axis.facing == facing) {
                return axis;
            }
        }
        return DOWN_UP;
    }

    /** Legacy saves stored {@link Direction#getName()}. */
    public static TurbinePlacementAxis fromLegacyDirectionName(String name) {
        Direction parsed = Direction.byName(name);
        return fromFacing(parsed);
    }

    public static TurbinePlacementAxis fromId(String id) {
        if (id == null || id.isEmpty()) {
            return DOWN_UP;
        }
        for (TurbinePlacementAxis axis : values()) {
            if (axis.id.equals(id)) {
                return axis;
            }
        }
        return DOWN_UP;
    }

    /** Shell face on either end of the turbine along the placement (growth) axis. */
    public boolean isShellCapWorld(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return switch (facing.getAxis()) {
            case Y -> y == minY || y == maxY;
            case Z -> z == minZ || z == maxZ;
            case X -> x == minX || x == maxX;
            default -> y == minY || y == maxY;
        };
    }

    /** Shell cell left open when open-top is enabled (cap on the {@link #facing()} side). */
    public boolean isOpenEndCapWorld(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return switch (facing) {
            case UP -> y == maxY;
            case DOWN -> y == minY;
            case SOUTH -> z == maxZ;
            case NORTH -> z == minZ;
            case EAST -> x == maxX;
            case WEST -> x == minX;
            default -> y == maxY;
        };
    }

    public boolean isShellCapLocal(int lx, int ly, int lz, int w, int h, int d) {
        return switch (facing.getAxis()) {
            case Y -> ly == 0 || ly == h - 1;
            case Z -> lz == 0 || lz == d - 1;
            case X -> lx == 0 || lx == w - 1;
            default -> ly == 0 || ly == h - 1;
        };
    }

    public boolean isOpenEndCapLocal(int lx, int ly, int lz, int w, int h, int d) {
        return switch (facing) {
            case UP -> ly == h - 1;
            case DOWN -> ly == 0;
            case SOUTH -> lz == d - 1;
            case NORTH -> lz == 0;
            case EAST -> lx == w - 1;
            case WEST -> lx == 0;
            default -> ly == h - 1;
        };
    }

    public int closureShellCoordLocal(int w, int h, int d, int coilLayerCount) {
        int interiorAlong = switch (facing.getAxis()) {
            case Y -> TurbineRodSpaceLayout.interiorHeight(h);
            case Z -> TurbineRodSpaceLayout.interiorDepth(d);
            case X -> TurbineRodSpaceLayout.interiorWidth(w);
            default -> TurbineRodSpaceLayout.interiorHeight(h);
        };
        int coils = TurbineRodSpaceLayout.appliedCoilLayerCount(interiorAlong, coilLayerCount);
        return 1 + TurbineRodControllerLayout.closureInteriorY(interiorAlong, coils);
    }

    public boolean isClosureShellLocal(int lx, int ly, int lz, int closureCoord) {
        return switch (facing.getAxis()) {
            case Y -> ly == closureCoord;
            case Z -> lz == closureCoord;
            case X -> lx == closureCoord;
            default -> ly == closureCoord;
        };
    }
}
