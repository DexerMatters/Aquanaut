package com.dexer.aquanaut.common.block;

import net.minecraft.core.BlockPos;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Merge rules for the dissection table.
 *
 * <p>
 * The three table models shipped with the mod are the same bench at three sizes: the 1x1 base
 * table, a 2x1 bench that is two base tables placed side by side along X, and a 2x2 bench that is
 * four tables in a square. This class is the single source of truth for that resolution. It is a
 * pure function over an occupancy predicate so both the client renderer and the server-side logic
 * agree, and so the rules can be unit tested without a level.
 *
 * <p>
 * Only exact, axis-aligned rectangles merge, and only from a <em>free north-west corner</em>: a cell
 * can grow a bench when the cells to its west, north and north-west are empty. That single rule
 * guarantees two merged benches can never overlap (so no z-fighting and no double benches), keeps
 * every resolution a pure local function of the four cells around it, and makes the master
 * unambiguous. Anything that does not satisfy it stays a 1x1 table, which is why a bench attached
 * to the east side of another bench stays as separate tables.
 *
 * <p>
 * A bench may run either way round: two tables placed along X give a 2x1 bench and two placed along
 * Z give the same bench turned a quarter turn. Because a free north-west corner is not enough to
 * disambiguate the two orientations (an L of three tables would let one corner grow both an X pair
 * and a Z pair, and the two would overlap), an X pair is always preferred and a Z pair additionally
 * requires the cell to its east to be free. An L therefore resolves to the western pair plus one
 * loose table, never to two crossing benches.
 */
public final class DissectionTableMultiblock {

    /** The merged rectangle: {@code origin} is the north-west (minimum x/z) cell. */
    public record Group(BlockPos origin, int widthX, int depthZ) {
        public Group {
            if (widthX < 1 || depthZ < 1) {
                throw new IllegalArgumentException("A dissection table group must cover at least one block");
            }
        }

        public int area() {
            return this.widthX * this.depthZ;
        }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= this.origin.getX() && pos.getX() < this.origin.getX() + this.widthX
                    && pos.getZ() >= this.origin.getZ() && pos.getZ() < this.origin.getZ() + this.depthZ;
        }

        public boolean isMaster(BlockPos pos) {
            return this.origin.equals(pos);
        }

        /** Geo model suffix: {@code ""}, {@code "_x2"} or {@code "_x4"}. */
        public String modelSuffix() {
            return switch (this.area()) {
                case 4 -> "_x4";
                case 2 -> "_x2";
                default -> "";
            };
        }
    }

    /** Layouts in the order they are preferred, largest first, X pairs before Z pairs. */
    private static final int[][] LAYOUTS = {{2, 2}, {2, 1}, {1, 2}, {1, 1}};

    private DissectionTableMultiblock() {
    }

    /**
     * Resolves the group that {@code pos} belongs to, or an empty optional when {@code pos} is not
     * an occupied table.
     */
    public static Optional<Group> resolve(Predicate<BlockPos> occupied, BlockPos pos) {
        if (!occupied.test(pos)) {
            return Optional.empty();
        }

        for (int[] layout : LAYOUTS) {
            int widthX = layout[0];
            int depthZ = layout[1];
            for (int dx = 0; dx < widthX; dx++) {
                for (int dz = 0; dz < depthZ; dz++) {
                    BlockPos origin = pos.offset(-dx, 0, -dz);
                    if (!isFreeCorner(occupied, origin) || !covers(occupied, origin, widthX, depthZ)) {
                        continue;
                    }
                    // A Z pair must not claim a corner that an X pair could use, or the two
                    // orientations would both grow from the same cell and overlap.
                    if (depthZ > widthX && occupied.test(origin.east())) {
                        continue;
                    }
                    return Optional.of(new Group(origin, widthX, depthZ));
                }
            }
        }

        // Not covered by any bench: a plain 1x1 table.
        return Optional.of(new Group(pos, 1, 1));
    }

    /** The rule that keeps merged benches from ever overlapping. */
    private static boolean isFreeCorner(Predicate<BlockPos> occupied, BlockPos origin) {
        return !occupied.test(origin.west())
                && !occupied.test(origin.north())
                && !occupied.test(origin.west().north());
    }

    private static boolean covers(Predicate<BlockPos> occupied, BlockPos origin, int widthX, int depthZ) {
        for (int x = 0; x < widthX; x++) {
            for (int z = 0; z < depthZ; z++) {
                if (!occupied.test(origin.offset(x, 0, z))) {
                    return false;
                }
            }
        }
        return true;
    }
}
