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

    /** Default: top-to-bottom rotor (U-D). */
    public static final int DEFAULT_INDEX = UP_DOWN.ordinal();

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
            return UP_DOWN;
        }
        return all[index];
    }

    public static TurbinePlacementAxis fromFacing(Direction facing) {
        if (facing == null) {
            return UP_DOWN;
        }
        for (TurbinePlacementAxis axis : values()) {
            if (axis.facing == facing) {
                return axis;
            }
        }
        return UP_DOWN;
    }

    /** Legacy saves stored {@link Direction#getName()}. */
    public static TurbinePlacementAxis fromLegacyDirectionName(String name) {
        Direction parsed = Direction.byName(name);
        return fromFacing(parsed);
    }

    public static TurbinePlacementAxis fromId(String id) {
        if (id == null || id.isEmpty()) {
            return UP_DOWN;
        }
        for (TurbinePlacementAxis axis : values()) {
            if (axis.id.equals(id)) {
                return axis;
            }
        }
        return UP_DOWN;
    }
}
