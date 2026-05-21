package net.unfamily.colossal_reactors.turbine;

import java.util.ArrayList;
import java.util.List;

/**
 * Placement of the single {@link net.unfamily.colossal_reactors.block.ModBlocks#TURBINE_ROD_CONTROLLER}
 * on the rod-zone closure layer (top Y of the rotor section, below the coil zone).
 */
public final class TurbineRodControllerLayout {

    public record Center(int rx, int rz) {}

    private TurbineRodControllerLayout() {}

    /** All geometrically valid rod-controller cells on the closure layer (rod-space indices). */
    public static List<Center> validCenters(int rw, int rd) {
        List<Center> out = new ArrayList<>(4);
        if (rw <= 0 || rd <= 0) {
            return out;
        }
        int cx = rw / 2;
        int cz = rd / 2;
        boolean oddW = (rw & 1) == 1;
        boolean oddD = (rd & 1) == 1;
        int x0 = oddW ? cx : cx - 1;
        int x1 = oddW ? cx : cx;
        int z0 = oddD ? cz : cz - 1;
        int z1 = oddD ? cz : cz;
        for (int rx = x0; rx <= x1; rx++) {
            for (int rz = z0; rz <= z1; rz++) {
                out.add(new Center(rx, rz));
            }
        }
        return out;
    }

    /**
     * Best-centered valid cell for even/odd footprints (min distance to geometric center of rod space).
     */
    public static Center bestPrimaryCenter(int rw, int rd) {
        List<Center> centers = validCenters(rw, rd);
        if (centers.isEmpty()) {
            return new Center(0, 0);
        }
        if (centers.size() == 1) {
            return centers.getFirst();
        }
        float targetX = (rw - 1) / 2.0f;
        float targetZ = (rd - 1) / 2.0f;
        Center best = centers.getFirst();
        float bestDist = Float.MAX_VALUE;
        for (Center c : centers) {
            float dx = c.rx() - targetX;
            float dz = c.rz() - targetZ;
            float dist = dx * dx + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                best = c;
            }
        }
        return best;
    }

    /** @deprecated Use {@link #bestPrimaryCenter}. */
    public static Center roundedPrimaryCenter(int rw, int rd) {
        return bestPrimaryCenter(rw, rd);
    }

    /** @deprecated Use {@link #bestPrimaryCenter}; kept for compatibility. */
    public static Center primaryCenter(int rw, int rd) {
        return bestPrimaryCenter(rw, rd);
    }

    public static boolean isValidCenter(int rx, int rz, int rw, int rd) {
        for (Center c : validCenters(rw, rd)) {
            if (c.rx() == rx && c.rz() == rz) {
                return true;
            }
        }
        return false;
    }

    /** Interior index of the closure deck (below coil fill along growth). */
    public static int closureInteriorY(int interiorHeight, int coilLayers) {
        return TurbineRodSpaceLayout.closureInteriorIndex(interiorHeight, coilLayers);
    }

    /** World Y of the rotor closure deck (ceiling of rod zone / floor of coil zone). */
    public static int closureWorldY(int minY, int interiorHeight, int coilLayers) {
        return minY + closureInteriorY(interiorHeight, coilLayers);
    }

    /** World X for rod-space column {@code rx} (first interior cell is {@code minX + 1}). */
    public static int closureWorldX(int minX, int rx) {
        return minX + 1 + rx;
    }

    public static int closureWorldZ(int minZ, int rz) {
        return minZ + 1 + rz;
    }

    public static int worldToRodSpaceX(int minX, int worldX) {
        return worldX - minX - 1;
    }

    public static int worldToRodSpaceZ(int minZ, int worldZ) {
        return worldZ - minZ - 1;
    }

    public static boolean isRodControllerWorldCell(int worldX, int worldZ, int minX, int minZ, Center center) {
        return worldX == closureWorldX(minX, center.rx()) && worldZ == closureWorldZ(minZ, center.rz());
    }
}
